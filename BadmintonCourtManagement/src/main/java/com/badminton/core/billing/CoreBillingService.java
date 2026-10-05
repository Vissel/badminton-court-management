package com.badminton.core.billing;

import com.badminton.constant.GameConstant;
import com.badminton.constant.PayType;
import com.badminton.constant.ServiceConstants;
import com.badminton.core.billing.print.BillPdfService;
import com.badminton.core.billing.print.EscPosNetworkPrinter;
import com.badminton.core.billing.print.ReceiptComposer;
import com.badminton.core.billing.print.ReceiptDocument;
import com.badminton.entity.*;
import com.badminton.enums.EInvoiceStatus;
import com.badminton.enums.InvoiceItemType;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.enums.PrinterMode;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.billing.BuyerInfo;
import com.badminton.model.dto.AllocateDebitPaymentRequest;
import com.badminton.model.dto.AllocateDebitPaymentResponse;
import com.badminton.model.dto.DebitPayDTO;
import com.badminton.model.dto.PaymentDTO;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.repository.BillConfigRepository;
import com.badminton.repository.InvoiceItemRepository;
import com.badminton.repository.InvoiceRepository;
import com.badminton.repository.InvoiceSeriesRepository;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.billing.BillConfigRequest;
import com.badminton.requestmodel.billing.BillListRequest;
import com.badminton.response.PageResponse;
import com.badminton.response.billing.BillConfigResponse;
import com.badminton.response.billing.BillItemResponse;
import com.badminton.response.billing.BillResponse;
import com.badminton.response.billing.ReceiptResponse;
import com.badminton.service.SessionServiceImpl;
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
import java.util.List;
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
    private SessionServiceImpl sessionService;
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
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.CHECKOUT);
        invoice.setSession(savedPlayer.getSession());
        invoice.setAvailablePlayer(savedPlayer);
        invoice.setPlayer(savedPlayer.getPlayer());
        invoice.setPayType(savedPlayer.getPayType());
        applyBuyerInfo(invoice, paymentDTO.getBuyer(), savedPlayer.getPlayer().getPlayerName());

        BigDecimal total = BigDecimal.ZERO;
        for (ServiceDTO service : paymentDTO.getServices() != null ? paymentDTO.getServices() : List.<ServiceDTO>of()) {
            InvoiceItem item = toItem(service);
            if (item == null) {
                continue;
            }
            invoice.addItem(item);
            if (item.getItemType() == InvoiceItemType.COURT_FEE
                    || item.getItemType() == InvoiceItemType.SERVICE
                    || item.getItemType() == InvoiceItemType.RENT_BY_TIME
                    || item.getItemType() == InvoiceItemType.DEBT_PAID) {
                total = total.add(item.getAmount());
            }
        }

        invoice.setTotal(total);
        // collectAmount = Σ all lines = charges - advance paid earlier - new debt.
        invoice.setCollectAmount(total.add(deductionSum(invoice)));
        applyVat(invoice, resolveVatRate(paymentDTO.getVatRate()));

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
        Invoice invoice = new Invoice();
        invoice.setInvoiceType(InvoiceType.DEBT_SETTLEMENT);
        invoice.setPayType(allocation.getPaymentMethod() != null ? allocation.getPaymentMethod() : PayType.CASH);
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

    private InvoiceItem toItem(ServiceDTO service) {
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
        return new InvoiceItem(type, name, qty, unitPrice, amount);
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
     * VAT is price-inclusive: net = gross / (1 + rate/100), vat = gross - net.
     * Computed on gross charges (the {@code total} column).
     */
    private void applyVat(Invoice invoice, BigDecimal vatRate) {
        invoice.setVatRate(vatRate);
        BigDecimal gross = invoice.getTotal();
        if (vatRate == null || vatRate.compareTo(BigDecimal.ZERO) <= 0 || gross == null) {
            invoice.setVatRate(vatRate != null ? vatRate : BigDecimal.ZERO);
            invoice.setSubtotal(gross != null ? gross : BigDecimal.ZERO);
            invoice.setVatAmount(BigDecimal.ZERO);
            return;
        }
        BigDecimal net = gross.divide(BigDecimal.ONE.add(vatRate.divide(HUNDRED)),
                0, RoundingMode.HALF_UP);
        invoice.setSubtotal(net);
        invoice.setVatAmount(gross.subtract(net));
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
        invoice.setIssuedAt(sessionService.getUTCPlus7Instant());
        invoice.setCurrency(MoneyUtils.CURRENCY_VN);
        return invoiceRepository.save(invoice);
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
        Assert.isTrue(!InvoiceStatus.VOIDED.equals(invoice.getStatus()), "Bill is already voided");
        Assert.isTrue(StringUtils.isNotBlank(reason), "Void reason must not be blank");
        Assert.isTrue(invoice.getEinvoiceStatus() == EInvoiceStatus.NONE
                || invoice.getEinvoiceStatus() == EInvoiceStatus.FAILED,
                "Cannot void a bill with a live e-invoice — cancel the e-invoice first");
        invoice.setStatus(InvoiceStatus.VOIDED);
        invoice.setVoidedBy(currentUser());
        invoice.setVoidedAt(sessionService.getUTCPlus7Instant());
        invoice.setVoidReason(reason);
        return toBillResponseDetail(invoiceRepository.save(invoice));
    }

    @Transactional
    public BillResponse markPrinted(Long billId) throws BusinessException {
        Invoice invoice = loadInvoice(billId);
        invoice.setPrintCount(invoice.getPrintCount() + 1);
        invoice.setLastPrintedAt(sessionService.getUTCPlus7Instant());
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
        invoice.setLastPrintedAt(sessionService.getUTCPlus7Instant());
        return toBillResponseDetail(invoiceRepository.save(invoice));
    }

    /** Connectivity probe for the configured network printer. */
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
        if (request.getAutoPrint() != null)
            config.setAutoPrint(request.getAutoPrint());
        config.setUpdatedBy(currentUser());
        return toConfigResponse(billConfigRepository.save(config));
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
        res.setPrintCount(invoice.getPrintCount());
        res.setNote(invoice.getNote());
        return res;
    }

    private BillItemResponse toItemResponse(InvoiceItem item) {
        return new BillItemResponse(item.getLineNo(),
                item.getItemType() != null ? item.getItemType().name() : null,
                item.getItemName(), item.getQty(), item.getUnitPrice(), item.getAmount(), item.getNote());
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
        res.setAutoPrint(config.isAutoPrint());
        return res;
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
