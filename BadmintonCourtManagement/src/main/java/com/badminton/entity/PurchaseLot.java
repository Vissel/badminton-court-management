package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * One wholesale intake batch: what was bought, when, and at what unit cost.
 */
@Entity
@Table(name = "purchase_lot")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long lotId;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    /**
     * Quantity in the purchased unit ({@link #unit}).
     */
    private int quantity;

    /**
     * Unit the quantity was bought in - base or package label (e.g. quả, ống).
     */
    private String unit;

    /**
     * {@code quantity} converted to the item's base unit. This is what the
     * stock_movement ledger records.
     */
    @Column(name = "base_quantity")
    private int baseQuantity;

    /**
     * Cost per purchased unit (per ống when bought by package).
     */
    @Column(name = "unit_cost")
    private BigDecimal unitCost;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    private String supplier;

    private String note;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;
}
