package com.badminton.enums;

/**
 * Stockable goods categories tracked in {@code inventory_item}.
 * Stockable services sold to players are modeled as {@link #GOODS};
 * non-stockable pricing rows (costInPerson, rentByTime) never get an item.
 */
public enum InventoryItemType {
    SHUTTLE_BALL,
    GOODS
}
