package com.badminton.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * One ledger line of the stock movement history.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementResponse {

    private long movementId;

    private int itemId;

    private String itemName;

    private String movementType;

    private int quantityDelta;

    private BigDecimal unitCost;

    private String refType;

    private Long refId;

    private String note;

    private Timestamp createdDate;
}
