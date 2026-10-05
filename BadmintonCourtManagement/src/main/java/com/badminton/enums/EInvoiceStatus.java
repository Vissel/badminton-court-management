package com.badminton.enums;

/**
 * Lifecycle of the e-invoice submission to the provider (MISA meInvoice).
 */
public enum EInvoiceStatus {
    /** Not submitted to the provider. */
    NONE,
    /** Submitted, awaiting confirmation. */
    PENDING,
    /** Published on the provider — legal e-invoice number assigned. */
    ISSUED,
    /** Provider rejected or call failed — retryable. */
    FAILED,
    /** Cancelled on the provider. */
    CANCELLED
}
