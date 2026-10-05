package com.badminton.entity;

import com.badminton.enums.InvoiceItemType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A single bill line. {@code itemName} is a snapshot — it stays correct even
 * after the catalog row is deactivated/re-inserted by a price change.
 */
@Entity
@Table(name = "invoice_item")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private InvoiceItemType itemType;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(precision = 8, scale = 2)
    private BigDecimal qty = BigDecimal.ONE;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 250)
    private String note;

    public InvoiceItem(InvoiceItemType itemType, String itemName, BigDecimal amount) {
        this.itemType = itemType;
        this.itemName = itemName;
        this.amount = amount;
    }

    public InvoiceItem(InvoiceItemType itemType, String itemName, BigDecimal qty,
                       BigDecimal unitPrice, BigDecimal amount) {
        this(itemType, itemName, amount);
        this.qty = qty;
        this.unitPrice = unitPrice;
    }
}
