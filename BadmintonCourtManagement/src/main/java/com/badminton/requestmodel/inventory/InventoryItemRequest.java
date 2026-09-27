package com.badminton.requestmodel.inventory;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Create/update payload for a stockable inventory item.
 */
@Data
public class InventoryItemRequest {

    private String itemName;

    /**
     * SHUTTLE_BALL | GOODS
     */
    private String itemType;

    /**
     * Base unit (quả, chai...).
     */
    private String unit;

    /**
     * Package unit for wholesale intake (ống...); empty clears it.
     */
    private String packageUnit;

    /**
     * Base units per package; required when packageUnit is set.
     */
    private Integer unitsPerPackage;

    private BigDecimal retailPrice;

    private Boolean isActive;
}
