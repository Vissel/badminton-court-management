package com.badminton.requestmodel.billing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * One correction line. {@code amount} is signed: positive = charge added,
 * negative = charge removed.
 */
@Data
public class AdjustItemRequest {
    private String itemName;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}
