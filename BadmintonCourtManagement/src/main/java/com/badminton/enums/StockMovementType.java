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
     * Shuttle balls consumed while start game, or add into game, or rental
     */
    GAME_CONSUMPTION,
    /**
     * Goods sold to a player at checkout (negative delta).
     * Shuttle ball when a game finish.
     */
    RETAIL_SALE,
    /**
     * Manual stock-take correction (signed delta, reason required).
     */
    ADJUSTMENT,
    /**
     * Returned goods (positive delta).
     * shuttle ball return when terminated game.
     */
    RETURN
}
