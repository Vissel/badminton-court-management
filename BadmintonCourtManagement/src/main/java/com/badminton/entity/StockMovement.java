package com.badminton.entity;

import com.badminton.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Append-only stock ledger. Stock on hand for an item = SUM(quantity_delta).
 */
@Entity
@Table(name = "stock_movement")
@Getter
@Setter
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long movementId;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type")
    private StockMovementType movementType;

    @Column(name = "quantity_delta")
    private int quantityDelta;

    /**
     * Set on PURCHASE_IN rows; used to replay a weighted average cost.
     */
    @Column(name = "unit_cost")
    private BigDecimal unitCost;

    /**
     * Reference to the business event, e.g. GAME for {@code game_id}.
     */
    @Column(name = "ref_type")
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    private String note;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;
}
