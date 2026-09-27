package com.badminton.enums;

/**
 * Action resolved for each imported product row during preview.
 */
public enum ImportAction {
    /**
     * Product name not found in DB - create a new row.
     */
    ADD,
    /**
     * Active product found with a different cost - deactivate old row and create a new one
     * so historical references stay intact.
     */
    UPDATE,
    /**
     * Inactive product found with the same name - reactivate it (and update cost if changed).
     */
    REACTIVATE,
    /**
     * Active product found with identical name and cost - nothing to do.
     */
    SKIP,
    /**
     * Row failed validation - never applied.
     */
    ERROR
}
