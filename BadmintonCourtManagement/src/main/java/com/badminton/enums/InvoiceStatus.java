package com.badminton.enums;

/**
 * Bills are never deleted — a voided bill keeps its number for audit.
 */
public enum InvoiceStatus {
    ISSUED,
    VOIDED
}
