package com.badminton.enums;

/**
 * Import mode controlling how products missing from the uploaded file are treated.
 */
public enum ProductImportMode {
    /**
     * Default - rows absent from the file are left untouched.
     */
    MERGE,
    /**
     * Active products absent from the file are deactivated
     * (system pricing rows are always protected).
     */
    REPLACE
}
