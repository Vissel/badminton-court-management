package com.badminton.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Per-row result of a stock-intake preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockIntakeRowResponse {

    private int rowNumber;

    private String name;

    private Integer quantity;

    /**
     * Resolved purchase unit label (e.g. ống, quả).
     */
    private String unit;

    /**
     * Base units that will be added to stock.
     */
    private Integer baseQuantity;

    private BigDecimal unitCost;

    private LocalDate purchaseDate;

    private String itemType;

    /**
     * ADD / UPDATE / ERROR.
     */
    private String action;

    private String message;
}
