package com.badminton.enums;

import lombok.Getter;

/**
 * Workbook sheets supported by the product import/export contract.
 */
@Getter
public enum ProductSheet {
    SHUTTLE_BALL("ShuttleBall"),
    SERVICE("Service");

    private final String sheetName;

    ProductSheet(String sheetName) {
        this.sheetName = sheetName;
    }
}
