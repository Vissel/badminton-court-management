package com.badminton.enums;

/**
 * Kind of stock ledger entry. Stock on hand = SUM(quantity_delta) per item.
 */
public enum StockMovementType {
    /**
     * Wholesale purchase intake (positive delta, carries unit_cost).
     */
    PURCHASE_IN,
    /**
     * Shuttle balls consumed by a finished game (negative delta).
     */
    GAME_CONSUMPTION,
    /**
     * Goods sold to a player at checkout (negative delta).
     */
    RETAIL_SALE,
    /**
     * Manual stock-take correction (signed delta, reason required).
     */
    ADJUSTMENT,
    /**
     * Returned goods (positive delta).
     */
    RETURN
}
