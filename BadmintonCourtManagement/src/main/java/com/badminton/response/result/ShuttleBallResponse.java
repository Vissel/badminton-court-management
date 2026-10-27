package com.badminton.response.result;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.ShuttleBall;
import com.badminton.util.MoneyUtils;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Objects;

@Data
public class ShuttleBallResponse {
    private String shuttleName;
    private float cost;
    private String costFormat;
    private final String currency = MoneyUtils.CURRENCY_VN;
    private boolean selected;

    /** Linked stock item id; null when the ball is not linked to inventory. */
    private Integer itemId;

    /**
     * Current stock in base units (quả); null when the ball is not linked to
     * inventory.
     */
    private Long stockOnHand;

    /** True when stockOnHand is non-null and <= 10. */
    private Boolean lowStock;

    /** True when the ball is sold out (stockOnHand <= 0). */
    private boolean outOfStock;

    /**
     * Stock expressed with the package unit, e.g. "5 ống + 3 quả";
     * null when the item has no package config.
     */
    private String stockBreakdown;

    /** Catalog row state: false → Ngừng bán. */
    private boolean active = true;

    /** Base unit from Quản lý kho (quả); null when unlinked. */
    private String unit;

    private String packageUnit;

    private Integer unitsPerPackage;

    /** Sell price owned by Quản lý kho; null → "Thiếu giá bán". */
    private BigDecimal retailPrice;

    private String retailPriceFormat;

    public ShuttleBallResponse(String shuttleName, float cost, boolean isSelected) {
        this.shuttleName = shuttleName;
        this.cost = cost;
        this.costFormat = MoneyUtils.formatToVND(cost);
        this.selected = isSelected;
    }

    public ShuttleBallResponse(ShuttleBall ball) {
        this(ball.getShuttleName(), ball.getCost(), ball.isSelected());
        this.active = ball.isActive();
        this.itemId = ball.getItem() != null ? ball.getItem().getItemId() : null;
        applyItem(ball.getItem());
    }

    private void applyItem(InventoryItem item) {
        if (item == null) {
            return;
        }
        this.unit = item.getUnit();
        this.packageUnit = item.getPackageUnit();
        this.unitsPerPackage = item.getUnitsPerPackage();
        this.retailPrice = item.getRetailPrice();
        if (item.getRetailPrice() != null) {
            this.retailPriceFormat = MoneyUtils.formatToVND(item.getRetailPrice().doubleValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ShuttleBallResponse))
            return false;
        ShuttleBallResponse that = (ShuttleBallResponse) o;
        return cost == that.cost && Objects.equals(shuttleName, that.shuttleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shuttleName, cost);
    }
}
