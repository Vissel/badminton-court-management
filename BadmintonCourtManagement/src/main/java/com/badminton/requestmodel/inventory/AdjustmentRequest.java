package com.badminton.requestmodel.inventory;

import lombok.Data;

/**
 * Manual stock correction (stock-take). {@code quantityDelta} is signed;
 * {@code reason} is mandatory for audit.
 */
@Data
public class AdjustmentRequest {

    private Integer itemId;

    private Integer quantityDelta;

    private String reason;
}
