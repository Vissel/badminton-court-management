package com.badminton.enums;

/**
 * Which money-collecting flow produced the bill.
 */
public enum InvoiceType {
    /**
     * Player session checkout (payToPlayer) — covers court fee, services,
     * rent-by-time, debts settled.
     */
    CHECKOUT,
    /**
     * Standalone debt settlement (/api/v1/debit/pay) outside a session checkout.
     */
    DEBT_SETTLEMENT,
    /**
     * Correction bill carrying only the delta against an issued bill (điều chỉnh).
     */
    ADJUSTMENT,
    /** Full replacement of an issued bill; the original is voided (thay thế). */
    REPLACEMENT
}
