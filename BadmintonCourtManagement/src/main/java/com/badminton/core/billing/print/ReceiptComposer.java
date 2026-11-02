package com.badminton.core.billing.print;

import com.badminton.constant.PayType;
import com.badminton.entity.BillConfig;
import com.badminton.entity.Invoice;
import com.badminton.entity.InvoiceItem;
import com.badminton.enums.CorrectionType;
import com.badminton.enums.InvoiceItemType;
import com.badminton.enums.InvoiceKind;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.util.MoneyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the {@link ReceiptDocument} layout shared by all bill renderers
 * (ESC/POS raster, PDF). Keep VN wording identical to the FE receipt view.
 */
@Component
public class ReceiptComposer {

    // issuedAt instants are stored already shifted +7h — format in UTC.
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneOffset.UTC);

    public ReceiptDocument compose(Invoice invoice, BillConfig config) {
        ReceiptDocument doc = new ReceiptDocument();

        // ── Seller block ──
        doc.add(ReceiptDocument.Line.builder()
                .left(StringUtils.isNotBlank(config.getBusinessName()) ? config.getBusinessName() : "Sân cầu lông")
                .style(ReceiptDocument.Style.CENTER_BOLD).build());
        if (StringUtils.isNotBlank(config.getAddress())) {
            doc.add(center(config.getAddress(), ReceiptDocument.Style.CENTER));
        }
        if (StringUtils.isNotBlank(config.getPhone())) {
            doc.add(center("ĐT: " + config.getPhone(), ReceiptDocument.Style.CENTER));
        }
        if (StringUtils.isNotBlank(config.getTaxCode())) {
            doc.add(center("MST: " + config.getTaxCode(), ReceiptDocument.Style.CENTER));
        }

        doc.addSeparator();
        doc.add(center(invoiceTitle(config), ReceiptDocument.Style.TITLE));
        doc.add(center("Số: " + invoice.getBillNo(), ReceiptDocument.Style.CENTER));
        if (invoice.getIssuedAt() != null) {
            doc.add(center(TS_FMT.format(invoice.getIssuedAt()), ReceiptDocument.Style.CENTER));
        }
        if (invoice.getCorrectionType() != null || invoice.getInvoiceType() == InvoiceType.ADJUSTMENT
                || invoice.getInvoiceType() == InvoiceType.REPLACEMENT) {
            doc.add(center(correctionLabel(invoice), ReceiptDocument.Style.CENTER_BOLD));
            if (invoice.getRelatedInvoice() != null) {
                doc.add(center("HĐ gốc: " + invoice.getRelatedInvoice().getBillNo(),
                        ReceiptDocument.Style.SMALL));
            }
        }

        // ── Buyer block ──
        doc.addSeparator();
        doc.add(pair("Khách hàng", buyerName(invoice), ReceiptDocument.Style.NORMAL));
        if (StringUtils.isNotBlank(invoice.getBuyerCompany())) {
            doc.add(pair("Đơn vị", invoice.getBuyerCompany(), ReceiptDocument.Style.NORMAL));
        }
        if (StringUtils.isNotBlank(invoice.getBuyerTaxCode())) {
            doc.add(pair("MST", invoice.getBuyerTaxCode(), ReceiptDocument.Style.NORMAL));
        }
        if (StringUtils.isNotBlank(invoice.getBuyerAddress())) {
            doc.add(pair("Địa chỉ", invoice.getBuyerAddress(), ReceiptDocument.Style.NORMAL));
        }
        doc.add(pair("Thu ngân", invoice.getIssuedBy(), ReceiptDocument.Style.NORMAL));
        doc.add(pair("Thanh toán", payTypeLabel(invoice.getPayType()), ReceiptDocument.Style.NORMAL));
        // G-13: transfer receipts carry the receiving account for reconciliation.
        String bankLine = bankLine(config);
        if (invoice.getPayType() == PayType.TRANSFER && bankLine != null) {
            doc.add(pair("Ngân hàng", bankLine, ReceiptDocument.Style.SMALL));
        }

        // ── Items ──
        doc.addSeparator();
        List<InvoiceItem> items = invoice.getItems() != null ? invoice.getItems() : List.of();
        items.stream()
                .sorted(Comparator.comparingInt(InvoiceItem::getLineNo))
                .forEach(item -> {
                    String sub = item.getQty() != null
                            && item.getQty().compareTo(BigDecimal.ONE) > 0
                            && item.getUnitPrice() != null
                                    ? item.getQty().stripTrailingZeros().toPlainString()
                                            + " x " + MoneyUtils.formatToVND(item.getUnitPrice().doubleValue())
                                    : null;
                    doc.add(ReceiptDocument.Line.builder()
                            .left(item.getItemName())
                            .right(MoneyUtils.formatToVND(item.getAmount().doubleValue()))
                            .sub(sub)
                            .style(ReceiptDocument.Style.NORMAL)
                            .build());
                });

        // ── Totals ──
        doc.addSeparator();
        doc.add(pair("Tổng cộng", MoneyUtils.formatToVND(invoice.getTotal().doubleValue()),
                ReceiptDocument.Style.BOLD));
        if (invoice.getVatRate() != null && invoice.getVatRate().compareTo(BigDecimal.ZERO) > 0) {
            doc.add(pair("  Trong đó VAT (" + invoice.getVatRate().stripTrailingZeros().toPlainString() + "%)",
                    MoneyUtils.formatToVND(invoice.getVatAmount().doubleValue()),
                    ReceiptDocument.Style.SMALL));
            doc.add(pair("  Giá chưa VAT",
                    MoneyUtils.formatToVND(invoice.getSubtotal().doubleValue()),
                    ReceiptDocument.Style.SMALL));
        }
        doc.add(pair("Thực thu", MoneyUtils.formatToVND(invoice.getCollectAmount().doubleValue()),
                ReceiptDocument.Style.BOLD));

        if (InvoiceStatus.VOIDED.equals(invoice.getStatus())) {
            doc.add(center("*** ĐÃ HUỶ ***", ReceiptDocument.Style.TITLE));
        }
        if (StringUtils.isNotBlank(invoice.getEinvoiceNo())) {
            doc.add(pair("HĐĐT", invoice.getEinvoiceNo(), ReceiptDocument.Style.NORMAL));
        }

        // ── Buyer retrieval QR (Điều 11) ──
        String lookup = lookupUrl(invoice, config);
        if (StringUtils.isNotBlank(lookup)) {
            doc.addSeparator();
            doc.setQrPayload(lookup);
            doc.setQrCaption(lookup);
            doc.add(center("Tra cứu hoá đơn", ReceiptDocument.Style.CENTER_BOLD));
        }

        if (StringUtils.isNotBlank(config.getBillFooter())) {
            doc.addSeparator();
            for (String line : config.getBillFooter().split("\\R")) {
                doc.add(center(line, ReceiptDocument.Style.CENTER));
            }
        }
        doc.add(center("Cảm ơn quý khách!", ReceiptDocument.Style.CENTER));
        return doc;
    }

    /** G-15: the printed title follows the configured invoice kind. */
    private String invoiceTitle(BillConfig config) {
        return config.getInvoiceKind() == InvoiceKind.GTGT
                ? "HOÁ ĐƠN GIÁ TRỊ GIA TĂNG"
                : "HOÁ ĐƠN BÁN HÀNG";
    }

    private String correctionLabel(Invoice invoice) {
        return invoice.getCorrectionType() == CorrectionType.THAY_THE
                ? "HOÁ ĐƠN THAY THẾ"
                : "HOÁ ĐƠN ĐIỀU CHỈNH";
    }

    /**
     * Retrieval link shown as a QR. Prefers the provider URL stored on the bill,
     * then builds one from the configured base pattern.
     */
    private String lookupUrl(Invoice invoice, BillConfig config) {
        if (StringUtils.isNotBlank(invoice.getEinvoiceLookupUrl())) {
            return invoice.getEinvoiceLookupUrl();
        }
        String base = config.getEinvoiceLookupBase();
        if (StringUtils.isBlank(base) || StringUtils.isBlank(invoice.getEinvoiceNo())) {
            return null;
        }
        return base.replace("{no}", invoice.getEinvoiceNo())
                .replace("{series}", StringUtils.defaultString(config.getEinvoiceSeries()))
                .replace("{taxCode}", StringUtils.defaultString(config.getTaxCode()));
    }

    private String buyerName(Invoice invoice) {
        if (StringUtils.isNotBlank(invoice.getBuyerName())) {
            return invoice.getBuyerName();
        }
        return invoice.getPlayer() != null ? invoice.getPlayer().getPlayerName() : "";
    }

    private String payTypeLabel(PayType payType) {
        if (payType == null) {
            return "";
        }
        return payType == PayType.TRANSFER ? "Chuyển khoản" : "Tiền mặt";
    }

    /** "{bank} - {account}" from the venue config, or null when neither is set. */
    private String bankLine(BillConfig config) {
        if (StringUtils.isBlank(config.getBankName()) && StringUtils.isBlank(config.getBankAccount())) {
            return null;
        }
        String name = StringUtils.defaultString(config.getBankName());
        String account = StringUtils.defaultString(config.getBankAccount());
        return StringUtils.isBlank(account) ? name : name + " - " + account;
    }

    private ReceiptDocument.Line center(String text, ReceiptDocument.Style style) {
        return ReceiptDocument.Line.builder().left(text).style(style).build();
    }

    private ReceiptDocument.Line pair(String left, String right, ReceiptDocument.Style style) {
        return ReceiptDocument.Line.builder().left(left).right(right).style(style).build();
    }
}
