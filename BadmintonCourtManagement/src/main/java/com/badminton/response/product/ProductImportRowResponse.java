package com.badminton.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One classified row of a product import preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportRowResponse {

    /**
     * 1-based row number inside the sheet.
     */
    private int row;

    /**
     * Sheet name: ShuttleBall | Service.
     */
    private String sheet;

    private String name;

    private BigDecimal cost;

    /**
     * ADD | UPDATE | REACTIVATE | SKIP | ERROR
     */
    private String action;

    private String message;
}
