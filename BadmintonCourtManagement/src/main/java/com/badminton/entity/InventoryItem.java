package com.badminton.entity;

import com.badminton.enums.InventoryItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Stable identity for a stockable good. Stock is tracked per item via
 * {@code stock_movement}, independent of catalog price rows that get
 * deactivated/re-inserted on price changes.
 */
@Entity
@Table(name = "inventory_item")
@Getter
@Setter
@NoArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int itemId;

    @Column(name = "item_name")
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type")
    private InventoryItemType itemType;

    /**
     * Base unit the ledger counts in (e.g. quả, chai).
     */
    private String unit;

    /**
     * Optional package unit used for wholesale intake (e.g. ống).
     * Null → item only supports base-unit intake.
     */
    @Column(name = "package_unit")
    private String packageUnit;

    /**
     * How many base units one package contains (e.g. 12 quả / ống).
     * Required whenever {@link #packageUnit} is set.
     */
    @Column(name = "units_per_package")
    private Integer unitsPerPackage;

    /**
     * Default sell price used for margin calculation.
     */
    @Column(name = "retail_price")
    private BigDecimal retailPrice;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;

    public InventoryItem(String itemName, InventoryItemType itemType) {
        this.itemName = itemName;
        this.itemType = itemType;
        this.isActive = true;
    }
}
