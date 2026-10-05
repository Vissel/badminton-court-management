package com.badminton.enums;

/**
 * Which money-collecting flow produced the bill.
 */
public enum InvoiceType {
    /** Player session checkout (payToPlayer) — covers court fee, services, rent-by-time, debts settled. */
    CHECKOUT,
    /** Standalone debt settlement (/api/v1/debit/pay) outside a session checkout. */
    DEBT_SETTLEMENT
}
