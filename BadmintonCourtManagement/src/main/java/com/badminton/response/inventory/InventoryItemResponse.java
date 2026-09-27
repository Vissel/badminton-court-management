package com.badminton.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item row for the inventory page: stock on hand, weighted average cost,
 * retail price and derived margin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {

    private int itemId;

    private String itemName;

    private String itemType;

    private String unit;

    private BigDecimal retailPrice;

    private boolean active;

    /**
     * Package unit for intake (ống...), null when the item has none.
     */
    private String packageUnit;

    private Integer unitsPerPackage;

    /**
     * Stock counted in base units.
     */
    private long stockOnHand;

    /**
     * Human-readable stock, e.g. "63 quả (5 ống + 3 quả)".
     */
    private String stockLabel;

    private BigDecimal avgCost;

    /**
     * {@code retailPrice - avgCost}; null when either side is unknown.
     */
    private BigDecimal margin;

    private boolean lowStock;

    /**
     * True when stock on hand is zero or below — the row is sold out,
     * which is stronger than {@link #lowStock}.
     */
    private boolean outOfStock;

    /**
     * Most recent goods intake date (latest purchase_lot.purchase_date);
     * null when the item was never intaken.
     */
    private LocalDate lastPurchaseDate;

    /**
     * Total base units ever intaken (sum of all PURCHASE_IN movements).
     */
    private Long purchasedQuantity;

    /**
     * Package breakdown of {@link #purchasedQuantity}, e.g. "5 ống + 3 quả".
     */
    private String purchasedBreakdown;

    /**
     * Package-level breakdown of stock for display, e.g. "5 ống + 3 quả".
     * Null when the item has no package unit or stock is below one package.
     */
    public String getPackageBreakdown() {
        return getPackageBreakdownFor(stockOnHand);
    }

    /**
     * Package breakdown for an arbitrary base-unit quantity.
     */
    public String getPackageBreakdownFor(long quantity) {
        if (packageUnit == null || unitsPerPackage == null || unitsPerPackage <= 0
                || quantity <= 0) {
            return null;
        }
        long packages = quantity / unitsPerPackage;
        if (packages == 0) {
            return null;
        }
        long remainder = quantity % unitsPerPackage;
        String baseUnit = unit != null ? unit : "";
        return remainder == 0
                ? packages + " " + packageUnit
                : packages + " " + packageUnit + " + " + remainder + " " + baseUnit;
    }
}
