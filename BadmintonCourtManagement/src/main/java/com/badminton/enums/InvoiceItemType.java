package com.badminton.enums;

/**
 * Classification of a bill line, derived from the service name on the
 * player's services JSON (see ServiceConstants / GameConstant).
 */
public enum InvoiceItemType {
    /** "Tiền sân" — per-head court fee. */
    COURT_FEE,
    /** Generic add-on service (drinks, rentals of goods, etc.). */
    SERVICE,
    /** "Thuê theo giờ *" — hourly court rent lines. */
    RENT_BY_TIME,
    /** "Trả nợ" — revenue from settling old debts inside this payment. */
    DEBT_PAID,
    /** "Trả trước" — prepaid advance applied to this bill (negative amount). */
    ADVANCE_DEDUCT,
    /** "Ghi nợ" — amount carved out as new debt, not collected (negative amount). */
    DEBT_CREATED
}
