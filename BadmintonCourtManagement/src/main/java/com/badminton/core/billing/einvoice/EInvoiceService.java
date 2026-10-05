package com.badminton.core.billing.einvoice;

import com.badminton.constant.PayType;
import com.badminton.entity.BillConfig;
import com.badminton.entity.Invoice;
import com.badminton.entity.InvoiceItem;
import com.badminton.enums.EInvoiceStatus;
import com.badminton.enums.InvoiceItemType;
import com.badminton.enums.InvoiceStatus;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.repository.BillConfigRepository;
import com.badminton.repository.InvoiceRepository;
import com.badminton.service.SessionServiceImpl;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * Publish (or retry) the bill as an e-invoice. Failed submissions mark the
     * bill FAILED with the provider error so cashiers can retry.
     */
    @Transactional
    public Invoice issue(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        BillConfig config = loadConfig();

        Assert.isTrue(config.isEinvoiceEnabled(), "E-invoice is disabled in billing config");
        Assert.isTrue(client.isConfigured(), "MISA credentials are not configured (einvoice.misa.*)");
        Assert.isTrue(!InvoiceStatus.VOIDED.equals(invoice.getStatus()),
                "Cannot issue an e-invoice for a voided bill");
        Assert.isTrue(invoice.getEinvoiceStatus() == EInvoiceStatus.NONE
                || invoice.getEinvoiceStatus() == EInvoiceStatus.FAILED,
                "E-invoice already " + invoice.getEinvoiceStatus());

        try {
            JsonObject response = client.publish(buildPayload(invoice, config));
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
                markFailed(invoice, errorMsg != null ? errorMsg : "Provider rejected the invoice");
            } else {
                invoice.setEinvoiceStatus(EInvoiceStatus.ISSUED);
                invoice.setEinvoiceNo(invoiceNo);
                invoice.setEinvoiceRef(refId != null ? refId : invoice.getBillNo());
                invoice.setEinvoiceAt(sessionService.getUTCPlus7Instant());
                invoice.setEinvoiceError(null);
            }
        } catch (BusinessException e) {
            markFailed(invoice, e.getErrorMessage());
        }
        return invoiceRepository.save(invoice);
    }

    /** Refresh einvoice_* from the provider's status endpoint. */
    @Transactional
    public Invoice refreshStatus(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        Assert.isTrue(StringUtils.isNotBlank(invoice.getEinvoiceRef())
                || StringUtils.isNotBlank(invoice.getEinvoiceNo()),
                "Bill has not been submitted to the e-invoice provider");
        try {
            JsonObject response = client.status(
                    invoice.getEinvoiceRef() != null ? invoice.getEinvoiceRef() : invoice.getBillNo(),
                    invoice.getEinvoiceNo());
            String invoiceNo = firstNonBlank(response, "InvoiceNo", "InvoiceNumber", "InvoiceCode");
            if (StringUtils.isNotBlank(invoiceNo)) {
                invoice.setEinvoiceNo(invoiceNo);
                invoice.setEinvoiceStatus(EInvoiceStatus.ISSUED);
            }
            String statusText = firstNonBlank(response, "InvoiceStatusName", "StatusName", "Status");
            if (statusText != null && statusText.toLowerCase().contains("huỷ")) {
                invoice.setEinvoiceStatus(EInvoiceStatus.CANCELLED);
            }
        } catch (BusinessException e) {
            invoice.setEinvoiceError(e.getErrorMessage());
        }
        return invoiceRepository.save(invoice);
    }

    /** Download the provider-side PDF for a bill that has an e-invoice. */
    public byte[] downloadPdf(Long billId) throws BusinessException {
        Invoice invoice = loadBill(billId);
        Assert.isTrue(StringUtils.isNotBlank(invoice.getEinvoiceNo()),
                "Bill has no e-invoice number yet");
        return client.download(invoice.getEinvoiceNo());
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
        payload.put("InvoiceName", "Hóa đơn bán hàng");
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

        List<Map<String, Object>> detail = new ArrayList<>();
        List<InvoiceItem> items = invoice.getItems() != null ? invoice.getItems() : List.of();
        BigDecimal rate = invoice.getVatRate() != null ? invoice.getVatRate() : BigDecimal.ZERO;
        for (InvoiceItem item : items.stream()
                .sorted(Comparator.comparingInt(InvoiceItem::getLineNo)).toList()) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("ProductName", item.getItemName());
            line.put("Quantity", item.getQty());
            line.put("UnitPrice", item.getUnitPrice());
            line.put("Amount", item.getAmount());
            // Negative lines (advance, new debt) become "discount"-style rows —
            // providers differ; keep the signed amount so the total matches.
            line.put("IsDecrease", item.getAmount().signum() < 0);
            line.put("TaxRate", rate);
            line.put("VATAmount", lineVat(item.getAmount(), rate));
            detail.add(line);
        }
        payload.put("InvoiceDetail", detail);
        return payload;
    }

    private BigDecimal lineVat(BigDecimal amount, BigDecimal rate) {
        if (rate.compareTo(BigDecimal.ZERO) <= 0 || amount == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal net = amount.divide(BigDecimal.ONE.add(rate.divide(HUNDRED)), 0, RoundingMode.HALF_UP);
        return amount.subtract(net);
    }

    private void markFailed(Invoice invoice, String message) {
        invoice.setEinvoiceStatus(EInvoiceStatus.FAILED);
        invoice.setEinvoiceError(message != null && message.length() > 500
                ? message.substring(0, 500)
                : message);
    }

    private Invoice loadBill(Long billId) throws BusinessException {
        Assert.notNull(billId, "Bill id must not be null");
        return invoiceRepository.findById(billId)
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.INTERNAL_SERVER_ERROR,
                        "Bill not found: " + billId));
    }

    private BillConfig loadConfig() {
        return billConfigRepository.findFirstByOrderByConfigIdAsc().orElseGet(BillConfig::new);
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
