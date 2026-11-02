package com.badminton.entity;

import com.badminton.enums.InvoiceLogAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Internal-control trail for sensitive bill operations: who issued, voided,
 * adjusted, printed or changed the billing config, and what the provider
 * answered. Not a legal requirement — it makes disputes traceable.
 */
@Entity
@Table(name = "invoice_log")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long logId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "bill_no", length = 30)
    private String billNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceLogAction action;

    @Column(length = 50)
    private String actor;

    @Column(length = 45)
    private String ip;

    @Column(length = 250)
    private String detail;

    /** Provider request/response excerpt; truncated, never carries credentials. */
    @Column(name = "payload", length = 4000)
    private String payload;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at")
    private Instant createdAt;

    public InvoiceLog(InvoiceLogAction action) {
        this.action = action;
    }
}
