package com.badminton.core.billing.einvoice;

import com.badminton.constant.PayType;
import com.badminton.core.billing.InvoiceAuditLogger;
import com.badminton.entity.BillConfig;
import com.badminton.entity.Invoice;
import com.badminton.entity.InvoiceItem;
import com.badminton.enums.CorrectionType;
import com.badminton.enums.EInvoiceStatus;
import com.badminton.enums.InvoiceKind;
import com.badminton.enums.InvoiceLogAction;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.repository.BillConfigRepository;
import com.badminton.repository.InvoiceRepository;
import com.badminton.requestmodel.billing.SendEInvoiceRequest;
import com.badminton.service.SessionServiceImpl;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Publishes issued bills to the e-invoice provider (MISA meInvoice) and keeps
 * the bill's einvoice_* fields in sync. Deliberately a separate step from
 * payment — a provider outage must never roll a payment back.
 *
 * <p>
 * Each state transition commits in its own transaction so an aborted HTTP
 * call leaves the bill in {@link EInvoiceStatus#PENDING} instead of silently
 * looking never-submitted (which would invite a duplicate invoice).
 */
@Slf4j
@Service
public class EInvoiceService {

    // issuedAt instants are stored already shifted +7h — format in UTC.
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneOffset.UTC);
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private BillConfigRepository billConfigRepository;
    @Autowired
    private MisaMeInvoiceClient client;
    @Autowired
    private SessionServiceImpl sessionService;
    @Autowired
    private InvoiceAuditLogger auditLogger;
    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * Publish (or retry) the bill as an e-invoice. Failed submissions mark the
     * bill FAILED with the provider error so cashiers can retry.
     */
    public Invoice issue(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        BillConfig config = loadConfig();

        Assert.isTrue(config.isEinvoiceEnabled(), "Chức năng hoá đơn điện tử chưa được bật trong cấu hình.");
        Assert.isTrue(client.isConfigured(),
                "Chưa cấu hình tài khoản MISA (einvoice.misa.*). Vui lòng liên hệ quản trị viên.");
        Assert.isTrue(!InvoiceStatus.VOIDED.equals(invoice.getStatus()),
                "Không thể phát hành hoá đơn điện tử cho hoá đơn đã huỷ.");
        Assert.isTrue(invoice.getEinvoiceStatus() == EInvoiceStatus.NONE
                || invoice.getEinvoiceStatus() == EInvoiceStatus.FAILED,
                "Hoá đơn điện tử đã ở trạng thái " + invoice.getEinvoiceStatus() + ".");

        // Commit PENDING before the HTTP call: if the JVM dies mid-request the
        // bill still shows as submitted and cannot be re-sent accidentally.
        Invoice pending = inNewTransaction(() -> {
            invoice.setEinvoiceStatus(EInvoiceStatus.PENDING);
            invoice.setEinvoiceError(null);
            return invoiceRepository.save(invoice);
        });
        auditLogger.log(InvoiceLogAction.EINVOICE_SUBMIT, pending,
                "Gửi hoá đơn điện tử tới MISA: " + pending.getBillNo());

        Map<String, Object> payload = buildPayload(pending, config);
        try {
            JsonObject response = client.publish(payload);
            String invoiceNo = firstNonBlank(response, "InvoiceNo", "InvoiceNumber", "InvoiceCode");
            String refId = firstNonBlank(response, "RefID", "RefId", "TransactionID");
            String errorCode = firstNonBlank(response, "ErrorCode", "Code", "ResultCode");
            String errorMsg = firstNonBlank(response, "Description", "ErrorMessage", "Message");
            boolean success = !response.has("Success")
                    || !response.get("Success").isJsonNull() && response.get("Success").getAsBoolean();
            if (success && invoiceNo == null && errorCode != null) {
                success = false;
            }
            if (!success) {
                return markFailed(pending, errorMsg != null ? errorMsg : "Provider rejected the invoice", null);
            }
            String xml = firstNonBlank(response, "InvoiceXML", "EInvoiceXML", "Xml");
            Invoice issued = markIssued(pending, invoiceNo, refId, xml, config);
            deliverByEmail(issued, config);
            return issued;
        } catch (BusinessException e) {
            return markFailed(pending, e.getErrorMessage(), payload.toString());
        }
    }

    /** Refresh einvoice_* from the provider's status endpoint. */
    public Invoice refreshStatus(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        Assert.isTrue(StringUtils.isNotBlank(invoice.getEinvoiceRef())
                || StringUtils.isNotBlank(invoice.getEinvoiceNo()),
                "Hoá đơn chưa được gửi tới nhà cung cấp hoá đơn điện tử.");

        Invoice updated = inNewTransaction(() -> {
            Invoice target = loadBillQuietly(billId);
            try {
                JsonObject response = client.status(
                        target.getEinvoiceRef() != null ? target.getEinvoiceRef() : target.getBillNo(),
                        target.getEinvoiceNo());
                String invoiceNo = firstNonBlank(response, "InvoiceNo", "InvoiceNumber", "InvoiceCode");
                if (StringUtils.isNotBlank(invoiceNo)) {
                    target.setEinvoiceNo(invoiceNo);
                    target.setEinvoiceStatus(EInvoiceStatus.ISSUED);
                }
                String statusText = firstNonBlank(response, "InvoiceStatusName", "StatusName", "Status");
                if (statusText != null && statusText.toLowerCase().contains("huỷ")) {
                    target.setEinvoiceStatus(EInvoiceStatus.CANCELLED);
                }
                if (StringUtils.isBlank(target.getEinvoiceLookupUrl())) {
                    target.setEinvoiceLookupUrl(lookupUrl(target, loadConfig()));
                }
            } catch (BusinessException e) {
                target.setEinvoiceError(truncate(e.getErrorMessage(), 500));
            }
            return invoiceRepository.save(target);
        });
        auditLogger.log(InvoiceLogAction.EINVOICE_STATUS, updated,
                "Cập nhật trạng thái HĐĐT: " + updated.getEinvoiceStatus());
        return updated;
    }

    /** Download the provider-side PDF for a bill that has an e-invoice. */
    public byte[] downloadPdf(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        Assert.isTrue(StringUtils.isNotBlank(invoice.getEinvoiceNo()),
                "Hoá đơn chưa có số hoá đơn điện tử.");
        return client.download(invoice.getEinvoiceNo());
    }

    /**
     * Archived provider XML. Falls back to a fresh download when the bill was
     * issued before archiving existed, persisting the result for next time.
     */
    public String downloadXml(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        Assert.isTrue(StringUtils.isNotBlank(invoice.getEinvoiceNo()),
                "Hoá đơn chưa có số hoá đơn điện tử.");
        if (StringUtils.isNotBlank(invoice.getEinvoiceXml())) {
            return invoice.getEinvoiceXml();
        }
        String xml = client.downloadXml(invoice.getEinvoiceNo());
        inNewTransaction(() -> {
            Invoice target = loadBillQuietly(billId);
            target.setEinvoiceXml(xml);
            return invoiceRepository.save(target);
        });
        return xml;
    }

    /**
     * (Re)delivers the issued e-invoice to the buyer by email. Kept separate
     * from {@link #issue} so a mail problem never blocks the invoice itself.
     */
    public Invoice sendMail(Long billId, SendEInvoiceRequest request) throws BusinessException {
        Invoice invoice = loadBill(billId);
        Assert.isTrue(client.isConfigured(),
                "Chưa cấu hình tài khoản MISA (einvoice.misa.*). Vui lòng liên hệ quản trị viên.");
        Assert.isTrue(invoice.getEinvoiceStatus() == EInvoiceStatus.ISSUED,
                "Chỉ gửi được email cho hoá đơn điện tử đã phát hành thành công.");
        String email = request != null && StringUtils.isNotBlank(request.getEmail())
                ? request.getEmail().trim()
                : invoice.getBuyerEmail();
        Assert.isTrue(StringUtils.isNotBlank(email), "Chưa có email người mua để gửi hoá đơn.");

        try {
            client.sendMail(email, invoice.getEinvoiceRef() != null ? invoice.getEinvoiceRef() : invoice.getBillNo(),
                    invoice.getEinvoiceNo());
            Invoice saved = inNewTransaction(() -> {
                Invoice target = loadBillQuietly(billId);
                target.setBuyerEmail(email);
                return invoiceRepository.save(target);
            });
            auditLogger.log(InvoiceLogAction.EINVOICE_MAIL, saved, "Gửi email hoá đơn tới " + email);
            return saved;
        } catch (BusinessException e) {
            auditLogger.log(InvoiceLogAction.EINVOICE_MAIL, invoice,
                    "Gửi email hoá đơn thất bại: " + email, null, e.getErrorMessage());
            throw e;
        }
    }

    // ---------- state transitions ----------

    private Invoice markIssued(Invoice pending, String invoiceNo, String refId, String xml, BillConfig config) {
        Invoice saved = inNewTransaction(() -> {
            Invoice target = loadBillQuietly(pending.getInvoiceId());
            target.setEinvoiceStatus(EInvoiceStatus.ISSUED);
            target.setEinvoiceNo(invoiceNo);
            target.setEinvoiceRef(refId != null ? refId : target.getBillNo());
            target.setEinvoiceAt(sessionService.getUTCPlus7Instant());
            target.setEinvoiceError(null);
            target.setEinvoiceLookupUrl(lookupUrl(target, config));
            String archived = xml;
            if (StringUtils.isBlank(archived)) {
                archived = fetchXmlQuietly(invoiceNo);
            }
            if (StringUtils.isNotBlank(archived)) {
                target.setEinvoiceXml(archived);
            }
            if (StringUtils.isBlank(target.getEinvoicePdfUrl()) && StringUtils.isNotBlank(invoiceNo)) {
                target.setEinvoicePdfUrl(pdfUrl(config, invoiceNo));
            }
            return invoiceRepository.save(target);
        });
        auditLogger.log(InvoiceLogAction.EINVOICE_ISSUE, saved,
                "Phát hành HĐĐT thành công: " + saved.getEinvoiceNo());
        return saved;
    }

    private Invoice markFailed(Invoice pending, String message, String payload) {
        Invoice saved = inNewTransaction(() -> {
            Invoice target = loadBillQuietly(pending.getInvoiceId());
            target.setEinvoiceStatus(EInvoiceStatus.FAILED);
            target.setEinvoiceError(truncate(message, 500));
            return invoiceRepository.save(target);
        });
        auditLogger.log(InvoiceLogAction.EINVOICE_ISSUE, saved,
                "Phát hành HĐĐT thất bại: " + saved.getBillNo(), payload, message);
        return saved;
    }

    private void deliverByEmail(Invoice invoice, BillConfig config) {
        if (!config.isEinvoiceEnabled() || StringUtils.isBlank(invoice.getBuyerEmail())) {
            return;
        }
        try {
            client.sendMail(invoice.getBuyerEmail(),
                    invoice.getEinvoiceRef() != null ? invoice.getEinvoiceRef() : invoice.getBillNo(),
                    invoice.getEinvoiceNo());
            auditLogger.log(InvoiceLogAction.EINVOICE_MAIL, invoice,
                    "Tự động gửi email hoá đơn tới " + invoice.getBuyerEmail());
        } catch (BusinessException e) {
            // The invoice itself is already valid — record and move on.
            auditLogger.log(InvoiceLogAction.EINVOICE_MAIL, invoice,
                    "Tự động gửi email thất bại: " + invoice.getBuyerEmail(), null, e.getErrorMessage());
        }
    }

    private String fetchXmlQuietly(String invoiceNo) {
        if (StringUtils.isBlank(invoiceNo)) {
            return null;
        }
        try {
            return client.downloadXml(invoiceNo);
        } catch (BusinessException e) {
            log.warn("Cannot archive e-invoice XML for {}: {}", invoiceNo, e.getErrorMessage());
            return null;
        }
    }

    /** Buyer retrieval link encoded in the receipt QR (Điều 11). */
    private String lookupUrl(Invoice invoice, BillConfig config) {
        if (StringUtils.isBlank(invoice.getEinvoiceNo())) {
            return invoice.getEinvoiceLookupUrl();
        }
        String base = config.getEinvoiceLookupBase();
        if (StringUtils.isBlank(base)) {
            return invoice.getEinvoiceLookupUrl();
        }
        return base.replace("{no}", invoice.getEinvoiceNo())
                .replace("{series}", StringUtils.defaultString(config.getEinvoiceSeries()))
                .replace("{taxCode}", StringUtils.defaultString(config.getTaxCode()));
    }

    private String pdfUrl(BillConfig config, String invoiceNo) {
        String base = config.getEinvoiceLookupBase();
        if (StringUtils.isBlank(base)) {
            return null;
        }
        return base.replace("{no}", invoiceNo)
                .replace("{series}", StringUtils.defaultString(config.getEinvoiceSeries()))
                .replace("{taxCode}", StringUtils.defaultString(config.getTaxCode()));
    }

    // ---------- payload ----------

    /**
     * meInvoice publish payload. Field names follow the integration API; tune
     * here if the tenant's schema differs (series/template come from
     * bill_config, endpoints from application properties).
     */
    private Map<String, Object> buildPayload(Invoice invoice, BillConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("TransactionID", invoice.getBillNo());
        payload.put("TemplateCode", config.getEinvoiceTemplate());
        payload.put("InvoiceSeries", config.getEinvoiceSeries());
        payload.put("InvoiceDate",
                invoice.getIssuedAt() != null ? TS_FMT.format(invoice.getIssuedAt()) : null);
        // G-15: the title follows the configured invoice kind instead of a
        // hardcoded string — GTGT for VAT invoices, BAN_HANG for sales bills.
        payload.put("InvoiceName", invoiceName(config));
        payload.put("InvoiceTypeID", invoiceKindCode(config));
        payload.put("CurrencyID", invoice.getCurrency() != null ? invoice.getCurrency() : "VND");
        payload.put("ExchangeRate", 1);
        payload.put("PaymentMethodName",
                invoice.getPayType() == PayType.TRANSFER ? "TM/CK" : "TM");
        payload.put("BuyerName", invoice.getBuyerName());
        payload.put("BuyerCompanyName", invoice.getBuyerCompany());
        payload.put("BuyerTaxCode", invoice.getBuyerTaxCode());
        payload.put("BuyerAddress", invoice.getBuyerAddress());
        payload.put("BuyerEmail", invoice.getBuyerEmail());
        payload.put("IsBuyerOrg", StringUtils.isNotBlank(invoice.getBuyerCompany()));
        payload.put("SignType", "5"); // hóa đơn máy tính tiền — no digital signature
        payload.put("IsSendInvoice", StringUtils.isNotBlank(invoice.getBuyerEmail()));
        payload.put("ReceiverEmail", invoice.getBuyerEmail());
        payload.put("TotalAmount", invoice.getTotal());
        payload.put("TotalVATAmount", invoice.getVatAmount());
        payload.put("BankName", config.getBankName());
        payload.put("BankAccount", config.getBankAccount());
        applyCorrection(payload, invoice);

        List<Map<String, Object>> detail = new ArrayList<>();
        List<InvoiceItem> items = invoice.getItems() != null ? invoice.getItems() : List.of();
        BigDecimal fallbackRate = invoice.getVatRate() != null ? invoice.getVatRate() : BigDecimal.ZERO;
        for (InvoiceItem item : items.stream()
                .sorted(Comparator.comparingInt(InvoiceItem::getLineNo)).toList()) {
            BigDecimal amount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            // G-8: per-item VAT — the stored snapshot wins over the bill rate.
            BigDecimal rate = item.getVatRate() != null ? item.getVatRate() : fallbackRate;
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("ProductName", item.getItemName());
            line.put("Quantity", item.getQty());
            line.put("UnitPrice", item.getUnitPrice());
            line.put("Amount", amount);
            // Negative lines (advance, new debt, discount) become "decrease"
            // rows — providers differ; keep the signed amount so the total matches.
            line.put("IsDecrease", amount.signum() < 0);
            line.put("TaxRate", rate);
            line.put("VATAmount",
                    item.getVatAmount() != null ? item.getVatAmount() : lineVat(amount, rate));
            detail.add(line);
        }
        payload.put("InvoiceDetail", detail);
        return payload;
    }

    /**
     * G-1: a correction references the original provider number and states
     * whether the adjustment increases or decreases the declared amount.
     */
    private void applyCorrection(Map<String, Object> payload, Invoice invoice) {
        InvoiceType type = invoice.getInvoiceType();
        boolean correction = type == InvoiceType.ADJUSTMENT || type == InvoiceType.REPLACEMENT;
        if (!correction) {
            return;
        }
        Invoice origin = invoice.getRelatedInvoice();
        payload.put("IsAdjustment", true);
        payload.put("AdjustmentType", invoice.getCorrectionType() == CorrectionType.THAY_THE ? 2 : 1);
        payload.put("AdjustmentReason", invoice.getAdjustReason());
        if (origin != null) {
            payload.put("OriginInvoiceNo", origin.getEinvoiceNo());
            payload.put("OriginInvoiceRef", origin.getEinvoiceRef());
            payload.put("OriginTransactionID", origin.getBillNo());
            payload.put("OriginInvoiceDate",
                    origin.getIssuedAt() != null ? TS_FMT.format(origin.getIssuedAt()) : null);
        }
        BigDecimal total = invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO;
        payload.put("IsIncrease", total.signum() >= 0);
    }

    private String invoiceName(BillConfig config) {
        return config.getInvoiceKind() == InvoiceKind.GTGT
                ? "HÓA ĐƠN GIÁ TRỊ GIA TĂNG"
                : "HÓA ĐƠN BÁN HÀNG";
    }

    /** 1 = hoá đơn GTGT, 2 = hoá đơn bán hàng (meInvoice convention). */
    private int invoiceKindCode(BillConfig config) {
        return config.getInvoiceKind() == InvoiceKind.GTGT ? 1 : 2;
    }

    private BigDecimal lineVat(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal net = amount.divide(BigDecimal.ONE.add(rate.divide(HUNDRED)), 0, RoundingMode.HALF_UP);
        return amount.subtract(net);
    }

    // ---------- helpers ----------

    private Invoice inNewTransaction(java.util.function.Supplier<Invoice> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }

    private Invoice loadBill(Long billId) throws BusinessException {
        Assert.notNull(billId, "Mã hoá đơn không được để trống.");
        return invoiceRepository.findById(billId)
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                        "Bill not found: " + billId));
    }

    /**
     * Same lookup as {@link #loadBill} for the in-transaction helpers: the
     * {@code Supplier} callback cannot carry a checked failure, so a missing
     * bill surfaces as an unchecked state error there.
     */
    private Invoice loadBillQuietly(Long billId) {
        return invoiceRepository.findById(billId)
                .orElseThrow();
    }

    private BillConfig loadConfig() {
        return billConfigRepository.findFirstByOrderByConfigIdAsc().orElseGet(BillConfig::new);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String firstNonBlank(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key) && !json.get(key).isJsonNull()
                    && StringUtils.isNotBlank(json.get(key).getAsString())) {
                return json.get(key).getAsString();
            }
        }
        return null;
    }
}
