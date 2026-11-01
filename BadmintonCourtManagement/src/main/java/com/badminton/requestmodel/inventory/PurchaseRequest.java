package com.badminton.requestmodel.inventory;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Wholesale intake record. {@code itemId} may be null when {@code itemName} +
 * {@code itemType} identify (or create) the item instead.
 */
@Data
public class PurchaseRequest {

    private Integer itemId;

    private String itemName;

    /**
     * SHUTTLE_BALL | GOODS - required when creating a new item by name.
     */
    private String itemType;

    private LocalDate purchaseDate;

    /**
     * Unit the {@code quantity}/{@code unitCost} are expressed in:
     * {@code BASE} or {@code PACKAGE}. Null → PACKAGE when the item has a
     * package unit configured, else BASE.
     */
    private String unit;

    private Integer quantity;

    /**
     * Cost per purchased unit (per ống when {@code unit=PACKAGE}).
     */
    private BigDecimal unitCost;

    /**
     * Optional retail price per base unit; when present it updates the item.
     */
    private BigDecimal retailPrice;

    /**
     * Optional Đơn vị lẻ (base unit) applied to the item, e.g. quả.
     */
    private String baseUnit;

    /**
     * Optional Đơn vị sỉ (wholesale unit) applied to the item, e.g. ống.
     * Blank clears the package config of an existing item.
     */
    private String packageUnit;

    /**
     * Optional Quy đổi sỉ sang lẻ: base units per Đơn vị sỉ (e.g. 12).
     */
    private Integer unitsPerPackage;

    private String supplier;

    private String note;
}
