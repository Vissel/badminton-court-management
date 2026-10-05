package com.badminton.entity;

import com.badminton.enums.EInvoiceStatus;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import com.badminton.constant.PayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable bill document issued at payment time. Rows are never deleted —
 * a cancelled bill is marked VOIDED and keeps its {@code billNo} for audit.
 */
@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long invoiceId;

    @Column(name = "bill_no", nullable = false, unique = true, length = 30)
    private String billNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 20)
    private InvoiceType invoiceType;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne
    @JoinColumn(name = "ava_id")
    private AvailablePlayer availablePlayer;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private Player player;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "buyer_name", length = 120)
    private String buyerName;

    @Column(name = "buyer_company", length = 200)
    private String buyerCompany;

    @Column(name = "buyer_tax_code", length = 20)
    private String buyerTaxCode;

    @Column(name = "buyer_address", length = 300)
    private String buyerAddress;

    @Column(name = "buyer_email", length = 120)
    private String buyerEmail;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "vat_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal vatAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "collect_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal collectAmount;

    @Column(length = 10)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_type", length = 20, nullable = false)
    private PayType payType;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    @Column(name = "issued_by", nullable = false, length = 50)
    private String issuedBy;

    @Column(name = "issued_at", updatable = false)
    private Instant issuedAt;

    @Column(name = "voided_by", length = 50)
    private String voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", length = 250)
    private String voidReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "einvoice_status", length = 15, nullable = false)
    private EInvoiceStatus einvoiceStatus = EInvoiceStatus.NONE;

    @Column(name = "einvoice_no", length = 50)
    private String einvoiceNo;

    @Column(name = "einvoice_ref", length = 100)
    private String einvoiceRef;

    @Column(name = "einvoice_pdf_url", length = 500)
    private String einvoicePdfUrl;

    @Column(name = "einvoice_at")
    private Instant einvoiceAt;

    @Column(name = "einvoice_error", length = 500)
    private String einvoiceError;

    @Column(name = "print_count", nullable = false)
    private int printCount = 0;

    @Column(name = "last_printed_at")
    private Instant lastPrintedAt;

    @Column(length = 250)
    private String note;

    @Column(updatable = false, insertable = false)
    private Instant createdDate;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNo")
    private List<InvoiceItem> items = new ArrayList<>();

    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        item.setLineNo(items.size() + 1);
        items.add(item);
    }
}
