package com.badminton.core.billing;

import com.badminton.constant.GameConstant;
import com.badminton.constant.PayType;
import com.badminton.constant.ServiceConstants;
import com.badminton.core.billing.print.BillPdfService;
import com.badminton.core.billing.print.EscPosNetworkPrinter;
import com.badminton.core.billing.print.ReceiptComposer;
import com.badminton.core.billing.print.ReceiptDocument;
import com.badminton.core.session.CoreSession;
import com.badminton.entity.*;
import com.badminton.enums.*;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.billing.BuyerInfo;
import com.badminton.model.dto.*;
import com.badminton.repository.*;
import com.badminton.requestmodel.billing.AdjustBillRequest;
import com.badminton.requestmodel.billing.AdjustItemRequest;
import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.requestmodel.billing.ReplaceBillRequest;
import com.badminton.requestmodel.billing.ServiceVatRequest;
import com.badminton.requestmodel.billing.TaxReportRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.billing.BillConfigResponse;
import com.badminton.response.billing.BillItemResponse;
import com.badminton.response.billing.BillLogResponse;
import com.badminton.response.billing.BillResponse;
import com.badminton.response.billing.ReceiptResponse;
import com.badminton.response.billing.ServiceVatResponse;
import com.badminton.response.billing.TaxReportResponse;
import com.badminton.util.MoneyUtils;
import com.badminton.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Issues immutable, sequentially numbered bills inside the caller's payment
 * transaction — a bill failure rolls the payment back (project convention).
 */
@Slf4j
@Service
public class CoreBillingService {

    private static final String SERIES_KEY = "BILL";
    private static final String RENT_BY_TIME_PREFIX = "Thuê theo giờ";
    private static final int BILL_NO_PAD = 6;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String DISCOUNT_VN = "Chiết khấu";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InvoiceItemRepository invoiceItemRepository;
    @Autowired
    private InvoiceSeriesRepository invoiceSeriesRepository;
    @Autowired
    private BillConfigRepository billConfigRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private InvoiceLogRepository invoiceLogRepository;
    @Autowired
    private InvoiceAuditLogger auditLogger;
    @Autowired
    private CoreSession coreSession;
    @Autowired
    private ReceiptComposer receiptComposer;
    @Autowired
    private BillPdfService billPdfService;
    @Autowired
    private EscPosNetworkPrinter escPosNetworkPrinter;

    /**
     * Issue a CHECKOUT bill for a just-paid player. Must be called inside the
     * payment transaction — the series lock is only safe that way.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Invoice issueCheckoutBill(PaymentDTO paymentDTO, AvailablePlayer savedPlayer) {
        // A retried payment must never produce a second bill.
        Invoice replayed = replayIfAlreadyIssued(paymentDTO.getClientTransactionId());
        if (replayed != null) {
            return replayed;
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.CHECKOUT);
        invoice.setSession(savedPlayer.getSession());
        invoice.setAvailablePlayer(savedPlayer);
        invoice.setPlayer(savedPlayer.getPlayer());
        invoice.setPayType(savedPlayer.getPayType());
        invoice.setClientTxnId(StringUtils.trimToNull(paymentDTO.getClientTransactionId()));
        applyBuyerInfo(invoice, paymentDTO.getBuyer(), savedPlayer.getPlayer().getPlayerName());

        BigDecimal vatRate = resolveVatRate(paymentDTO.getVatRate());
        Map<String, BigDecimal> serviceRates = serviceVatRates();
        for (ServiceDTO service : paymentDTO.getServices() != null ? paymentDTO.getServices() : List.<ServiceDTO>of()) {
            InvoiceItem item = toItem(service, vatRate, serviceRates);
            if (item != null) {
                invoice.addItem(item);
            }
        }
        applyDiscount(invoice, paymentDTO.getDiscountAmount());

        invoice.setTotal(chargeSum(invoice));
        // collectAmount = Σ all lines = charges - advance paid earlier - new debt.
        invoice.setCollectAmount(invoice.getTotal().add(deductionSum(invoice)));
        applyVat(invoice, vatRate);

        return persist(invoice);
    }

    /**
     * Issue a DEBT_SETTLEMENT bill for a standalone debt payment
     * (/api/v1/debit/pay). Called inside the allocation transaction so the
     * bill and the debt payment commit or roll back together.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Invoice issueDebitSettlementBill(AllocateDebitPaymentRequest request,
            AllocateDebitPaymentResponse allocation) {
        Invoice replayed = replayIfAlreadyIssued(request.getClientTransactionId());
        if (replayed != null) {
            return replayed;
        }
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.DEBT_SETTLEMENT);
        invoice.setPayType(allocation.getPaymentMethod() != null ? allocation.getPaymentMethod() : PayType.CASH);
        invoice.setClientTxnId(StringUtils.trimToNull(request.getClientTransactionId()));
        userRepository.findAllByPlayerName(request.getPlayerName()).stream()
                .findFirst()
                .ifPresent(invoice::setPlayer);
        applyBuyerInfo(invoice, request.getBuyer(), request.getPlayerName());
        invoice.setNote(request.getNote());

        BigDecimal total = BigDecimal.ZERO;
        for (DebitPayDTO debitPay : request.getListDebitPay() != null ? request.getListDebitPay()
                : List.<DebitPayDTO>of()) {
            BigDecimal amount = debitPay.getPayAmount() != null
                    ? debitPay.getPayAmount().setScale(0, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            InvoiceItem item = new InvoiceItem(InvoiceItemType.DEBT_PAID,
                    ServiceConstants.PAY_DEBIT_VN + " " + debitPay.getDateTime(), amount);
            invoice.addItem(item);
            total = total.add(amount);
        }

        invoice.setTotal(total);
        invoice.setCollectAmount(total);
        applyVat(invoice, resolveVatRate(request.getVatRate()));

        return persist(invoice);
    }

    private void applyBuyerInfo(Invoice invoice, BuyerInfo buyer, String defaultBuyerName) {
        invoice.setBuyerName(buyer != null && StringUtils.isNotBlank(buyer.getBuyerName())
                ? buyer.getBuyerName()
                : defaultBuyerName);
        if (buyer != null) {
            invoice.setBuyerCompany(buyer.getCompany());
            invoice.setBuyerTaxCode(buyer.getTaxCode());
            invoice.setBuyerAddress(buyer.getAddress());
            invoice.setBuyerEmail(buyer.getEmail());
        }
    }

    private InvoiceItem toItem(ServiceDTO service, BigDecimal fallbackRate,
            Map<String, BigDecimal> serviceRates) {
        if (service == null || StringUtils.isBlank(service.getServiceName())) {
            return null;
        }
        String name = service.getServiceName();
        BigDecimal amount = BigDecimal.valueOf(service.getCost()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal qty = service.getQuantity() != null && service.getQuantity() > 0
                ? BigDecimal.valueOf(service.getQuantity())
                : BigDecimal.ONE;
        BigDecimal unitPrice = qty.compareTo(BigDecimal.ZERO) > 0
                ? amount.divide(qty, 2, RoundingMode.HALF_UP)
                : amount;

        InvoiceItemType type = classify(name);
        // Deduction lines store negative amounts so SUM(items) = collectAmount.
        if (type == InvoiceItemType.ADVANCE_DEDUCT || type == InvoiceItemType.DEBT_CREATED) {
            amount = amount.abs().negate();
        }
        InvoiceItem item = new InvoiceItem(type, name, qty, unitPrice, amount);
        item.setVatRate(resolveLineVatRate(name, fallbackRate, serviceRates));
        return item;
    }

    /**
     * Correction lines carry a signed amount: positive adds a charge, negative
     * removes one. Name keeps the classification so a corrected court fee is
     * still a COURT_FEE line.
     */
    private InvoiceItem toItem(AdjustItemRequest line, BigDecimal fallbackRate,
            Map<String, BigDecimal> serviceRates) {
        if (line == null || line.getAmount() == null || line.getAmount().signum() == 0) {
            return null;
        }
        String name = StringUtils.isNotBlank(line.getItemName()) ? line.getItemName().trim() : "Điều chỉnh";
        BigDecimal amount = line.getAmount().setScale(0, RoundingMode.HALF_UP);
        BigDecimal qty = line.getQty() != null && line.getQty().signum() > 0 ? line.getQty() : BigDecimal.ONE;
        BigDecimal unitPrice = line.getUnitPrice() != null
                ? line.getUnitPrice().setScale(2, RoundingMode.HALF_UP)
                : amount.divide(qty, 2, RoundingMode.HALF_UP);
        InvoiceItem item = new InvoiceItem(classify(name), name, qty, unitPrice, amount);
        item.setVatRate(resolveLineVatRate(name, fallbackRate, serviceRates));
        return item;
    }

    /**
     * Per-item VAT: a catalog row may carry its own rate (Setup page); rows
     * without one inherit the bill's rate.
     */
    private BigDecimal resolveLineVatRate(String serviceName, BigDecimal fallbackRate,
            Map<String, BigDecimal> serviceRates) {
        BigDecimal perItem = serviceName != null ? serviceRates.get(serviceName) : null;
        if (perItem != null) {
            return perItem.setScale(2, RoundingMode.HALF_UP);
        }
        return fallbackRate != null ? fallbackRate : BigDecimal.ZERO;
    }

    private Map<String, BigDecimal> serviceVatRates() {
        Map<String, BigDecimal> rates = new LinkedHashMap<>();
        for (com.badminton.entity.Service service : serviceRepository.findAllByIsActive(true)) {
            if (service.getSerName() != null && service.getVatRate() != null) {
                rates.put(service.getSerName(), service.getVatRate());
            }
        }
        return rates;
    }

    /**
     * Chiết khấu is a negative DISCOUNT line so the charged total stays
     * transparent.
     */
    private void applyDiscount(Invoice invoice, BigDecimal requestedDiscount) {
        if (requestedDiscount == null || requestedDiscount.signum() <= 0) {
            return;
        }
        BigDecimal discount = requestedDiscount.setScale(0, RoundingMode.HALF_UP);
        // A discount may not push the bill below zero.
        Assert.isTrue(discount.compareTo(chargeSum(invoice)) <= 0,
                "Chiết khấu không được lớn hơn tổng tiền hàng");
        invoice.addItem(new InvoiceItem(InvoiceItemType.DISCOUNT, DISCOUNT_VN, discount.negate()));
        invoice.setDiscountAmount(discount);
    }

    /** Gross charges = every line except the two deduction types. */
    private BigDecimal chargeSum(Invoice invoice) {
        return invoice.getItems().stream()
                .filter(i -> i.getItemType() != InvoiceItemType.ADVANCE_DEDUCT
                        && i.getItemType() != InvoiceItemType.DEBT_CREATED)
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice replayIfAlreadyIssued(String clientTxnId) {
        if (StringUtils.isBlank(clientTxnId)) {
            return null;
        }
        return invoiceRepository.findByClientTxnId(clientTxnId.trim()).orElse(null);
    }

    /** Exposed for the payment layer: same lookup, without creating a bill. */
    public Invoice findByClientTxnId(String clientTxnId) {
        return replayIfAlreadyIssued(clientTxnId);
    }

    private InvoiceItemType classify(String serviceName) {
        if (GameConstant.COST_IN_PERSON_VN.equals(serviceName)) {
            return InvoiceItemType.COURT_FEE;
        }
        if (serviceName.startsWith(RENT_BY_TIME_PREFIX)) {
            return InvoiceItemType.RENT_BY_TIME;
        }
        if (GameConstant.ADVANCE_PAYMENT_VN.equals(serviceName)) {
            return InvoiceItemType.ADVANCE_DEDUCT;
        }
        if (ServiceConstants.PAY_DEBIT_VN.equals(serviceName)) {
            return InvoiceItemType.DEBT_PAID;
        }
        if (ServiceConstants.CREATE_DEBIT_VN.equals(serviceName)) {
            return InvoiceItemType.DEBT_CREATED;
        }
        return InvoiceItemType.SERVICE;
    }

    private BigDecimal deductionSum(Invoice invoice) {
        return invoice.getItems().stream()
                .filter(i -> i.getItemType() == InvoiceItemType.ADVANCE_DEDUCT
                        || i.getItemType() == InvoiceItemType.DEBT_CREATED)
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * VAT is price-inclusive: line VAT = amount - amount / (1 + rate/100),
     * rounded to đồng. The bill's vat_amount is the sum of the line VATs, so
     * Σ line VAT == invoice.vatAmount exactly (no line-vs-total drift even
     * when catalog rows carry different rates).
     */
    private void applyVat(Invoice invoice, BigDecimal vatRate) {
        invoice.setVatRate(vatRate != null ? vatRate : BigDecimal.ZERO);
        BigDecimal gross = invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getAmount() == null || item.getAmount().signum() <= 0) {
                item.setVatAmount(BigDecimal.ZERO);
                continue;
            }
            BigDecimal rate = item.getVatRate() != null ? item.getVatRate() : invoice.getVatRate();
            item.setVatRate(rate);
            BigDecimal lineVat = lineVat(item.getAmount(), rate);
            item.setVatAmount(lineVat);
            vat = vat.add(lineVat);
        }
        invoice.setVatAmount(vat);
        invoice.setSubtotal(gross.subtract(vat));
    }

    private BigDecimal lineVat(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0 || amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal net = amount.divide(BigDecimal.ONE.add(rate.divide(HUNDRED)), 0, RoundingMode.HALF_UP);
        return amount.subtract(net);
    }

    private BigDecimal resolveVatRate(BigDecimal requestedRate) {
        if (requestedRate != null) {
            // Per-bill override is a privileged operation.
            Assert.isTrue(hasAdminRole(), "VAT rate override requires ADMINISTRATOR or ROOT role");
            Assert.isTrue(requestedRate.compareTo(BigDecimal.ZERO) >= 0
                    && requestedRate.compareTo(HUNDRED) < 0, "VAT rate must be in [0, 100)");
            return requestedRate;
        }
        return loadConfig().getVatRate();
    }

    private boolean hasAdminRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ROOT")
                        || a.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "system";
    }

    private Invoice persist(Invoice invoice) {
        invoice.setBillNo(nextBillNo(loadConfig().getBillPrefix()));
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setEinvoiceStatus(EInvoiceStatus.NONE);
        invoice.setIssuedBy(currentUser());
        invoice.setIssuedAt(coreSession.getUTCPlus7Instant());
        invoice.setCurrency(MoneyUtils.CURRENCY_VN);
        if (invoice.getDiscountAmount() == null) {
            invoice.setDiscountAmount(BigDecimal.ZERO);
        }
        Invoice saved = invoiceRepository.save(invoice);
        auditLogger.log(InvoiceLogAction.ISSUE, saved, "Phát hành hoá đơn " + saved.getBillNo());
        return saved;
    }

    /**
     * Gapless numbering: the series row is locked FOR UPDATE inside the
     * payment transaction — concurrent checkouts serialize here.
     */
    private String nextBillNo(String prefix) {
        InvoiceSeries series = invoiceSeriesRepository.findBySeriesKey(SERIES_KEY)
                .orElseGet(() -> invoiceSeriesRepository.save(new InvoiceSeries(SERIES_KEY)));
        series.setCurrentNo(series.getCurrentNo() + 1);
        invoiceSeriesRepository.save(series);
        String safePrefix = StringUtils.isNotBlank(prefix) ? prefix : "BL";
        return String.format("%s-%0" + BILL_NO_PAD + "d", safePrefix, series.getCurrentNo());
    }

    // ---------- queries / admin ----------

    public PageResponse<BillResponse> searchBills(BillListRequest request) {
        PageResponse<BillResponse> pageResponse = new PageResponse<>();
        Instant from = StringUtils.isNotBlank(request.getFromDate())
                ? TimeUtils.convertToInstant(request.getFromDate())
                : null;
        Instant to = StringUtils.isNotBlank(request.getToDate())
                ? TimeUtils.convertToInstant(request.getToDate())
                : null;
        InvoiceStatus status = parseEnum(InvoiceStatus.class, request.getStatus());
        InvoiceType type = parseEnum(InvoiceType.class, request.getInvoiceType());
        String keyword = StringUtils.isNotBlank(request.getKeyword()) ? request.getKeyword().trim() : null;

        // Pagination.current is 1-based across the app (see
        // CoreDebitService.buildPageable)
        int page = request.getPagination() != null ? Math.max(request.getPagination().getCurrent() - 1, 0) : 0;
        int size = request.getPagination() != null && request.getPagination().getPageSize() > 0
                ? request.getPagination().getPageSize()
                : 10;
        Page<Invoice> result = invoiceRepository.search(from, to, status, type,
                request.getSessionId(), keyword, PageRequest.of(page, size));

        pageResponse.setTotal(result.getTotalElements());
        pageResponse.setList(result.getContent().stream()
                .map(this::toBillResponse)
                .collect(Collectors.toList()));
        if (request.getPagination() != null) {
            request.getPagination().setTotalPage(result.getTotalPages());
            pageResponse.setPagination(request.getPagination());
        }
        return pageResponse;
    }

    public BillResponse getBill(Long billId) throws BusinessException {
        return toBillResponseDetail(loadInvoice(billId));
    }

    public ReceiptResponse getReceipt(Long billId) throws BusinessException {
        Invoice invoice = loadInvoice(billId);
        BillConfig config = loadConfig();
        ReceiptResponse receipt = new ReceiptResponse();
        receipt.setBill(toBillResponseDetail(invoice));
        receipt.setSeller(toConfigResponse(config));
        receipt.setPrinterMode(
                config.getPrinterMode() != null ? config.getPrinterMode().name() : PrinterMode.BROWSER.name());
        receipt.setPaperWidth(config.getPaperWidth());
        receipt.setAutoPrint(config.isAutoPrint());
        return receipt;
    }

    @Transactional
    public BillResponse voidBill(Long billId, String reason) throws BusinessException {
        Invoice invoice = loadInvoice(billId);
        Assert.isTrue(!InvoiceStatus.VOIDED.equals(invoice.getStatus()), "Hoá đơn này đã được huỷ trước đó.");
        Assert.isTrue(StringUtils.isNotBlank(reason), "Vui lòng nhập lý do huỷ hoá đơn.");
        // HĐĐT đã phát hành không có khái niệm "huỷ": phải lập hoá đơn điều
        // chỉnh/thay thế trước, rồi mới huỷ nội bộ.
        if (invoice.getEinvoiceStatus() == EInvoiceStatus.ISSUED
                || invoice.getEinvoiceStatus() == EInvoiceStatus.CANCELLED) {
            boolean hasCorrection = invoiceRepository.existsByRelatedInvoiceInvoiceId(invoice.getInvoiceId());
            Assert.isTrue(hasCorrection,
                    "Vui lòng lập hóa đơn điều chỉnh/thay thế trước khi huỷ.");
        }
        Assert.isTrue(invoice.getEinvoiceStatus() != EInvoiceStatus.PENDING,
                "Hoá đơn điện tử đang được gửi đi. Vui lòng cập nhật trạng thái HĐĐT rồi thử lại.");
        invoice.setStatus(InvoiceStatus.VOIDED);
        invoice.setVoidedBy(currentUser());
        invoice.setVoidedAt(coreSession.getUTCPlus7Instant());
        invoice.setVoidReason(reason);
        Invoice saved = invoiceRepository.save(invoice);
        auditLogger.log(InvoiceLogAction.VOID, saved, reason);
        return toBillResponseDetail(saved);
    }

    /**
     * Điều chỉnh: a new immutable bill carrying only the delta, linked to the
     * original. The original keeps its number and stays issued.
     */
    @Transactional
    public BillResponse issueAdjustmentBill(Long originalId, AdjustBillRequest request) throws BusinessException {
        Invoice original = loadInvoice(originalId);
        Assert.isTrue(StringUtils.isNotBlank(request.getReason()), "Vui lòng nhập lý do điều chỉnh.");
        Assert.isTrue(InvoiceStatus.ISSUED.equals(original.getStatus()),
                "Chỉ lập hoá đơn điều chỉnh cho hoá đơn đã phát hành.");
        List<AdjustItemRequest> lines = request.getItems() != null ? request.getItems() : List.of();
        Assert.isTrue(!lines.isEmpty(), "Vui lòng nhập nội dung điều chỉnh.");
        Invoice correction = buildCorrectionBill(original, InvoiceType.ADJUSTMENT,
                CorrectionType.DIEU_CHINH, request.getReason(), lines, null, request.getVatRate());
        return toBillResponseDetail(correction);
    }

    /**
     * Thay thế: a full replacement bill; the original is voided so the
     * corrected amounts are not counted twice in revenue reports.
     */
    @Transactional
    public BillResponse issueReplacementBill(Long originalId, ReplaceBillRequest request) throws BusinessException {
        Invoice original = loadInvoice(originalId);
        Assert.isTrue(StringUtils.isNotBlank(request.getReason()), "Vui lòng nhập lý do thay thế.");
        Assert.isTrue(InvoiceStatus.ISSUED.equals(original.getStatus()),
                "Chỉ lập hoá đơn thay thế cho hoá đơn đã phát hành.");
        List<AdjustItemRequest> lines = request.getItems() != null && !request.getItems().isEmpty()
                ? request.getItems()
                : toAdjustLines(original);

        Invoice replacement = buildCorrectionBill(original, InvoiceType.REPLACEMENT,
                CorrectionType.THAY_THE, request.getReason(), lines, request.getBuyer(), request.getVatRate());

        original.setStatus(InvoiceStatus.VOIDED);
        original.setVoidedBy(currentUser());
        original.setVoidedAt(coreSession.getUTCPlus7Instant());
        original.setVoidReason("Thay thế bởi " + replacement.getBillNo());
        invoiceRepository.save(original);
        auditLogger.log(InvoiceLogAction.VOID, original, "Thay thế bởi " + replacement.getBillNo());
        return toBillResponseDetail(replacement);
    }

    private Invoice buildCorrectionBill(Invoice original, InvoiceType type, CorrectionType correctionType,
            String reason, List<AdjustItemRequest> lines, BuyerInfo buyer,
            BigDecimal requestedRate) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(type);
        invoice.setCorrectionType(correctionType);
        invoice.setRelatedInvoice(original);
        invoice.setAdjustReason(StringUtils.abbreviate(reason.trim(), 250));
        invoice.setSession(original.getSession());
        invoice.setPlayer(original.getPlayer());
        invoice.setPayType(original.getPayType() != null ? original.getPayType() : PayType.CASH);
        copyBuyer(invoice, original, buyer);

        // A correction keeps the original's rate unless an admin overrides it.
        BigDecimal vatRate = requestedRate != null
                ? resolveVatRate(requestedRate)
                : (original.getVatRate() != null ? original.getVatRate() : resolveVatRate(null));
        Map<String, BigDecimal> serviceRates = serviceVatRates();
        for (AdjustItemRequest line : lines) {
            InvoiceItem item = toItem(line, vatRate, serviceRates);
            if (item != null) {
                invoice.addItem(item);
            }
        }
        Assert.isTrue(!invoice.getItems().isEmpty(), "Vui lòng nhập nội dung điều chỉnh.");

        invoice.setTotal(chargeSum(invoice));
        invoice.setCollectAmount(invoice.getTotal().add(deductionSum(invoice)));
        applyVat(invoice, vatRate);

        Invoice saved = persist(invoice);
        auditLogger.log(type == InvoiceType.ADJUSTMENT ? InvoiceLogAction.ADJUST : InvoiceLogAction.REPLACE,
                saved, reason + (" (" + original.getBillNo() + ")"));
        return saved;
    }

    private void copyBuyer(Invoice invoice, Invoice original, BuyerInfo buyer) {
        invoice.setBuyerName(original.getBuyerName());
        invoice.setBuyerCompany(original.getBuyerCompany());
        invoice.setBuyerTaxCode(original.getBuyerTaxCode());
        invoice.setBuyerAddress(original.getBuyerAddress());
        invoice.setBuyerEmail(original.getBuyerEmail());
        if (buyer != null) {
            applyBuyerInfo(invoice, buyer, original.getBuyerName());
        }
    }

    /** Copies the original lines so a replacement can default to them. */
    private List<AdjustItemRequest> toAdjustLines(Invoice original) {
        List<InvoiceItem> items = original.getItems() != null && !original.getItems().isEmpty()
                ? original.getItems()
                : invoiceItemRepository.findByInvoiceInvoiceIdOrderByLineNo(original.getInvoiceId());
        List<AdjustItemRequest> lines = new ArrayList<>();
        for (InvoiceItem item : items) {
            AdjustItemRequest line = new AdjustItemRequest();
            line.setItemName(item.getItemName());
            line.setQty(item.getQty());
            line.setUnitPrice(item.getUnitPrice());
            line.setAmount(item.getAmount());
            lines.add(line);
        }
        return lines;
    }

    @Transactional
    public BillResponse markPrinted(Long billId) throws BusinessException {
        Invoice invoice = loadInvoice(billId);
        invoice.setPrintCount(invoice.getPrintCount() + 1);
        invoice.setLastPrintedAt(coreSession.getUTCPlus7Instant());
        return toBillResponseDetail(invoiceRepository.save(invoice));
    }

    /**
     * Print through the configured channel: NETWORK pushes the rendered
     * receipt to the ESC/POS printer; BROWSER only counts the print (the FE
     * already ran window.print()). A printer failure never rolls the bill
     * back — it surfaces as a BusinessException to the caller.
     */
    @Transactional
    public BillResponse printBill(Long billId, String channel) throws BusinessException {
        Invoice invoice = loadInvoice(billId);
        BillConfig config = loadConfig();
        PrinterMode mode = StringUtils.isNotBlank(channel)
                ? parseEnum(PrinterMode.class, channel)
                : config.getPrinterMode();
        if (mode == null) {
            mode = PrinterMode.BROWSER;
        }
        if (mode == PrinterMode.NETWORK) {
            Assert.isTrue(StringUtils.isNotBlank(config.getPrinterIp()),
                    "Printer IP is not configured");
            int port = config.getPrinterPort() != null ? config.getPrinterPort() : 9100;
            int paper = config.getPaperWidth() != null ? config.getPaperWidth() : 80;
            ReceiptDocument doc = receiptComposer.compose(invoice, config);
            escPosNetworkPrinter.print(doc, config.getPrinterIp(), port, paper);
        }
        invoice.setPrintCount(invoice.getPrintCount() + 1);
        invoice.setLastPrintedAt(coreSession.getUTCPlus7Instant());
        return toBillResponseDetail(invoiceRepository.save(invoice));
    }

    /**
     * Connectivity probe for the configured network printer.
     */
    public void testPrinter() throws BusinessException {
        BillConfig config = loadConfig();
        Assert.isTrue(StringUtils.isNotBlank(config.getPrinterIp()), "Printer IP is not configured");
        int port = config.getPrinterPort() != null ? config.getPrinterPort() : 9100;
        escPosNetworkPrinter.testConnection(config.getPrinterIp(), port);
    }

    public byte[] renderPdf(Long billId) throws BusinessException {
        Invoice invoice = loadInvoice(billId);
        BillConfig config = loadConfig();
        int paper = config.getPaperWidth() != null ? config.getPaperWidth() : 80;
        return billPdfService.render(receiptComposer.compose(invoice, config), paper);
    }

    // ---------- config ----------

    public BillConfig loadConfig() {
        return billConfigRepository.findFirstByOrderByConfigIdAsc().orElseGet(BillConfig::new);
    }

    public BillConfigResponse getConfig() {
        return toConfigResponse(loadConfig());
    }

    @Transactional
    public BillConfigResponse updateConfig(BillConfigRequest request) {
        BillConfig config = billConfigRepository.findFirstByOrderByConfigIdAsc()
                .orElseGet(BillConfig::new);
        if (request.getBusinessName() != null)
            config.setBusinessName(request.getBusinessName());
        if (request.getTaxCode() != null)
            config.setTaxCode(request.getTaxCode());
        if (request.getAddress() != null)
            config.setAddress(request.getAddress());
        if (request.getPhone() != null)
            config.setPhone(request.getPhone());
        if (StringUtils.isNotBlank(request.getBillPrefix()))
            config.setBillPrefix(request.getBillPrefix());
        if (request.getVatRate() != null) {
            Assert.isTrue(request.getVatRate().compareTo(BigDecimal.ZERO) >= 0
                    && request.getVatRate().compareTo(HUNDRED) < 0, "VAT rate must be in [0, 100)");
            config.setVatRate(request.getVatRate());
        }
        if (request.getBillFooter() != null)
            config.setBillFooter(request.getBillFooter());
        if (StringUtils.isNotBlank(request.getPrinterMode())) {
            config.setPrinterMode(parseEnum(PrinterMode.class, request.getPrinterMode()));
        }
        if (request.getPrinterIp() != null)
            config.setPrinterIp(request.getPrinterIp());
        if (request.getPrinterPort() != null)
            config.setPrinterPort(request.getPrinterPort());
        if (request.getPaperWidth() != null)
            config.setPaperWidth(request.getPaperWidth());
        if (request.getEinvoiceEnabled() != null)
            config.setEinvoiceEnabled(request.getEinvoiceEnabled());
        if (request.getEinvoiceSeries() != null)
            config.setEinvoiceSeries(request.getEinvoiceSeries());
        if (request.getEinvoiceTemplate() != null)
            config.setEinvoiceTemplate(request.getEinvoiceTemplate());
        if (request.getInvoiceKind() != null) {
            InvoiceKind kind = parseEnum(InvoiceKind.class, request.getInvoiceKind());
            Assert.notNull(kind, "Loại hoá đơn không hợp lệ (GTGT hoặc BAN_HANG)");
            config.setInvoiceKind(kind);
        }
        if (request.getEinvoicePattern() != null)
            config.setEinvoicePattern(request.getEinvoicePattern());
        if (request.getBankName() != null)
            config.setBankName(request.getBankName());
        if (request.getBankAccount() != null)
            config.setBankAccount(request.getBankAccount());
        if (request.getEinvoiceLookupBase() != null)
            config.setEinvoiceLookupBase(request.getEinvoiceLookupBase());
        if (request.getAutoPrint() != null)
            config.setAutoPrint(request.getAutoPrint());
        config.setUpdatedBy(currentUser());
        BillConfig saved = billConfigRepository.save(config);
        auditLogger.log(InvoiceLogAction.CONFIG_UPDATE, null,
                "Cập nhật cấu hình hoá đơn: " + saved.getBusinessName());
        return toConfigResponse(saved);
    }

    // ---------- audit trail ----------

    /**
     * Internal-control history of one bill. Read-only; empty when the bill was
     * created before logging was introduced.
     */
    public List<BillLogResponse> getBillLogs(Long billId) throws BusinessException {
        loadInvoice(billId);
        return invoiceLogRepository.findByInvoiceIdOrderByLogIdAsc(billId).stream()
                .map(log -> new BillLogResponse(log.getLogId(),
                        log.getAction() != null ? log.getAction().name() : null,
                        log.getActor(), log.getIp(), log.getDetail(), log.getError(),
                        log.getCreatedAt() != null ? LOG_FMT.format(log.getCreatedAt()) : null))
                .collect(Collectors.toList());
    }

    // ---------- tax report ----------

    /**
     * Revenue + output-VAT summary for a period (bảng kê bán ra). Voided bills
     * are excluded so the figures match what was actually declared.
     */
    public TaxReportResponse taxReport(TaxReportRequest request) {
        String groupBy = StringUtils.isBlank(request.getGroupBy())
                ? "MONTH"
                : request.getGroupBy().trim().toUpperCase();
        Assert.isTrue("DAY".equals(groupBy) || "MONTH".equals(groupBy) || "QUARTER".equals(groupBy),
                "Kiểu nhóm báo cáo không hợp lệ (DAY, MONTH hoặc QUARTER)");
        LocalDate from = parseReportDate(request.getFromDate(), LocalDate.now().minusMonths(1));
        LocalDate to = parseReportDate(request.getToDate(), LocalDate.now());
        Assert.isTrue(!to.isBefore(from), "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");

        Instant fromInstant = from.atStartOfDay().atZone(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay().atZone(ZoneOffset.UTC).toInstant();

        Map<String, TaxReportResponse.Row> buckets = new LinkedHashMap<>();
        TaxReportResponse response = new TaxReportResponse();
        response.setFromDate(from.toString());
        response.setToDate(to.toString());
        response.setGroupBy(groupBy);

        for (Object[] row : invoiceRepository.findReportRows(fromInstant, toInstant, InvoiceStatus.VOIDED)) {
            Instant issuedAt = (Instant) row[0];
            BigDecimal gross = row[1] != null ? (BigDecimal) row[1] : BigDecimal.ZERO;
            BigDecimal net = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            BigDecimal vat = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            String period = reportPeriod(issuedAt, groupBy);
            TaxReportResponse.Row bucket = buckets.computeIfAbsent(period,
                    p -> new TaxReportResponse.Row(p, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
            bucket.setBillCount(bucket.getBillCount() + 1);
            bucket.setGross(bucket.getGross().add(gross));
            bucket.setNet(bucket.getNet().add(net));
            bucket.setVat(bucket.getVat().add(vat));
            response.setBillCount(response.getBillCount() + 1);
            response.setGross(response.getGross().add(gross));
            response.setNet(response.getNet().add(net));
            response.setVat(response.getVat().add(vat));
        }
        response.getRows().addAll(buckets.values());
        return response;
    }

    private String reportPeriod(Instant issuedAt, String groupBy) {
        LocalDate day = issuedAt == null ? LocalDate.now() : issuedAt.atZone(ZoneOffset.UTC).toLocalDate();
        switch (groupBy) {
            case "DAY":
                return DAY_FMT.format(issuedAt != null ? issuedAt : Instant.now());
            case "QUARTER":
                return day.getYear() + "-Q" + ((day.getMonthValue() - 1) / 3 + 1);
            default:
                return MONTH_FMT.format(issuedAt != null ? issuedAt : Instant.now());
        }
    }

    private LocalDate parseReportDate(String value, LocalDate fallback) {
        if (StringUtils.isBlank(value)) {
            return fallback;
        }
        String text = value.trim();
        if (text.length() > 10) {
            text = text.substring(0, 10);
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            Assert.isTrue(false, "Ngày không hợp lệ: " + value);
            return fallback;
        }
    }

    // ---------- per-service VAT ----------

    /**
     * Persists the VAT override of one service. A null rate clears the override
     * so the venue-wide rate applies again on the next bill.
     */
    @Transactional
    public ServiceVatResponse updateServiceVat(Long serviceId, ServiceVatRequest request) {
        Assert.notNull(serviceId, "Service id must not be null");
        com.badminton.entity.Service service = serviceRepository.findById(serviceId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại: " + serviceId));
        BigDecimal rate = request != null ? request.getVatRate() : null;
        if (rate != null) {
            Assert.isTrue(rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(HUNDRED) < 0,
                    "Thuế suất VAT phải trong khoảng [0, 100)");
        }
        service.setVatRate(rate);
        com.badminton.entity.Service saved = serviceRepository.save(service);
        ServiceVatResponse res = new ServiceVatResponse();
        res.setServiceId((long) saved.getSerId());
        res.setServiceName(saved.getSerName());
        res.setVatRate(saved.getVatRate());
        return res;
    }

    /** VAT overrides of every service, keyed by service name. */
    public List<ServiceVatResponse> serviceVatList() {
        List<ServiceVatResponse> list = new ArrayList<>();
        for (com.badminton.entity.Service service : serviceRepository.findAll()) {
            ServiceVatResponse res = new ServiceVatResponse();
            res.setServiceId((long) service.getSerId());
            res.setServiceName(service.getSerName());
            res.setVatRate(service.getVatRate());
            list.add(res);
        }
        return list;
    }

    // ---------- mapping ----------

    private Invoice loadInvoice(Long billId) throws BusinessException {
        Assert.notNull(billId, "Bill id must not be null");
        return invoiceRepository.findById(billId)
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                        "Bill not found: " + billId));
    }

    private BillResponse toBillResponse(Invoice invoice) {
        BillResponse res = toBillResponseDetailBase(invoice);
        res.setItems(null);
        return res;
    }

    private BillResponse toBillResponseDetail(Invoice invoice) {
        BillResponse res = toBillResponseDetailBase(invoice);
        List<InvoiceItem> items = invoice.getItems() != null && !invoice.getItems().isEmpty()
                ? invoice.getItems()
                : invoiceItemRepository.findByInvoiceInvoiceIdOrderByLineNo(invoice.getInvoiceId());
        res.setItems(items.stream().map(this::toItemResponse).collect(Collectors.toList()));
        return res;
    }

    private BillResponse toBillResponseDetailBase(Invoice invoice) {
        BillResponse res = new BillResponse();
        res.setBillId(invoice.getInvoiceId());
        res.setBillNo(invoice.getBillNo());
        res.setInvoiceType(invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : null);
        res.setSessionId(invoice.getSession() != null ? invoice.getSession().getSessionId() : null);
        res.setPlayerName(invoice.getPlayer() != null ? invoice.getPlayer().getPlayerName() : null);
        res.setBuyerName(invoice.getBuyerName());
        res.setBuyerCompany(invoice.getBuyerCompany());
        res.setBuyerTaxCode(invoice.getBuyerTaxCode());
        res.setBuyerAddress(invoice.getBuyerAddress());
        res.setBuyerEmail(invoice.getBuyerEmail());
        res.setSubtotal(invoice.getSubtotal());
        res.setVatRate(invoice.getVatRate());
        res.setVatAmount(invoice.getVatAmount());
        res.setTotal(invoice.getTotal());
        res.setCollectAmount(invoice.getCollectAmount());
        res.setCurrency(invoice.getCurrency());
        res.setPayType(invoice.getPayType() != null ? invoice.getPayType().name() : null);
        res.setStatus(invoice.getStatus() != null ? invoice.getStatus().name() : null);
        res.setIssuedBy(invoice.getIssuedBy());
        res.setIssuedAt(invoice.getIssuedAt() != null ? invoice.getIssuedAt().toString() : null);
        res.setVoidedBy(invoice.getVoidedBy());
        res.setVoidedAt(invoice.getVoidedAt() != null ? invoice.getVoidedAt().toString() : null);
        res.setVoidReason(invoice.getVoidReason());
        res.setEinvoiceStatus(invoice.getEinvoiceStatus() != null ? invoice.getEinvoiceStatus().name() : null);
        res.setEinvoiceNo(invoice.getEinvoiceNo());
        res.setEinvoicePdfUrl(invoice.getEinvoicePdfUrl());
        res.setEinvoiceError(invoice.getEinvoiceError());
        res.setDiscountAmount(invoice.getDiscountAmount());
        res.setEinvoiceLookupUrl(resolveLookupUrl(invoice));
        res.setEinvoiceXmlAvailable(StringUtils.isNotBlank(invoice.getEinvoiceXml()));
        Invoice related = invoice.getRelatedInvoice();
        res.setRelatedBillId(related != null ? related.getInvoiceId() : null);
        res.setRelatedBillNo(related != null ? related.getBillNo() : null);
        res.setCorrectionType(invoice.getCorrectionType() != null ? invoice.getCorrectionType().name() : null);
        res.setAdjustReason(invoice.getAdjustReason());
        if (invoice.getInvoiceId() != null) {
            res.setCorrected(invoiceRepository.existsByRelatedInvoiceInvoiceId(invoice.getInvoiceId()));
        }
        res.setPrintCount(invoice.getPrintCount());
        res.setNote(invoice.getNote());
        return res;
    }

    private BillItemResponse toItemResponse(InvoiceItem item) {
        BillItemResponse res = new BillItemResponse();
        res.setLineNo(item.getLineNo());
        res.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
        res.setItemName(item.getItemName());
        res.setQty(item.getQty());
        res.setUnitPrice(item.getUnitPrice());
        res.setAmount(item.getAmount());
        res.setVatRate(item.getVatRate());
        res.setVatAmount(item.getVatAmount());
        res.setNote(item.getNote());
        return res;
    }

    private BillConfigResponse toConfigResponse(BillConfig config) {
        BillConfigResponse res = new BillConfigResponse();
        res.setBusinessName(config.getBusinessName());
        res.setTaxCode(config.getTaxCode());
        res.setAddress(config.getAddress());
        res.setPhone(config.getPhone());
        res.setBillPrefix(config.getBillPrefix());
        res.setVatRate(config.getVatRate());
        res.setBillFooter(config.getBillFooter());
        res.setPrinterMode(config.getPrinterMode() != null ? config.getPrinterMode().name() : null);
        res.setPrinterIp(config.getPrinterIp());
        res.setPrinterPort(config.getPrinterPort());
        res.setPaperWidth(config.getPaperWidth());
        res.setEinvoiceEnabled(config.isEinvoiceEnabled());
        res.setEinvoiceSeries(config.getEinvoiceSeries());
        res.setEinvoiceTemplate(config.getEinvoiceTemplate());
        res.setInvoiceKind(config.getInvoiceKind() != null ? config.getInvoiceKind().name() : null);
        res.setEinvoicePattern(config.getEinvoicePattern());
        res.setBankName(config.getBankName());
        res.setBankAccount(config.getBankAccount());
        res.setEinvoiceLookupBase(config.getEinvoiceLookupBase());
        res.setAutoPrint(config.isAutoPrint());
        return res;
    }

    /**
     * Buyer retrieval link printed as a QR code (Điều 11 NĐ 254/2026). Prefers
     * the URL returned by the provider, then falls back to the configured base
     * pattern so the link exists even before the invoice is transmitted.
     */
    public String resolveLookupUrl(Invoice invoice) {
        if (StringUtils.isNotBlank(invoice.getEinvoiceLookupUrl())) {
            return invoice.getEinvoiceLookupUrl();
        }
        BillConfig config = loadConfig();
        String base = config.getEinvoiceLookupBase();
        if (StringUtils.isBlank(base) || StringUtils.isBlank(invoice.getEinvoiceNo())) {
            return null;
        }
        String url = base.replace("{no}", invoice.getEinvoiceNo())
                .replace("{series}", StringUtils.defaultString(config.getEinvoiceSeries()))
                .replace("{taxCode}", StringUtils.defaultString(config.getTaxCode()));
        return invoice.getEinvoiceLookupUrl() != null ? invoice.getEinvoiceLookupUrl() : url;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
