package com.badminton.enums;

/**
 * Sensitive operations recorded in {@code invoice_log} for internal control
 * (who issued/voided/printed/config-changed and what the provider answered).
 */
public enum InvoiceLogAction {
    ISSUE,
    VOID,
    ADJUST,
    REPLACE,
    PRINT,
    DISCOUNT,
    EINVOICE_ISSUE,
    EINVOICE_SUBMIT,
    EINVOICE_STATUS,
    EINVOICE_MAIL,
    EINVOICE_SEND,
    CONFIG_UPDATE
}
