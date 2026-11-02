package com.badminton.enums;

/**
 * Invoice title published to the provider. Depends on the venue's declared tax
 * method: khấu trừ → GTGT, trực tiếp/khoán → bán hàng.
 */
public enum InvoiceKind {
    /** "Hóa đơn giá trị gia tăng". */
    GTGT,
    /** "Hóa đơn bán hàng". */
    BAN_HANG
}
