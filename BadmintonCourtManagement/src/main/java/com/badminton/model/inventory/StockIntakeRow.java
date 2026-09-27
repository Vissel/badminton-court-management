package com.badminton.model.inventory;

import com.badminton.enums.InventoryItemType;
import com.badminton.enums.ImportAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One parsed row of the StockIntake sheet (Tên | Số lượng | Giá nhập | Ngày nhập).
 * {@link #itemRef} holds the resolved {@link com.badminton.entity.InventoryItem}
 * for UPDATE rows; null for ADD (new item) and ERROR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockIntakeRow {

    /**
     * 1-based row number inside the sheet.
     */
    private int rowNumber;

    private String name;

    private Integer quantity;

    /**
     * Purchase unit: raw text parsed from the Đơn vị column; after
     * classification it holds the resolved unit label (ống, quả...).
     */
    private String unit;

    /**
     * {@code quantity} converted to base units (set during classification).
     */
    private Integer baseQuantity;

    /**
     * Cost per purchased unit.
     */
    private BigDecimal unitCost;

    private LocalDate purchaseDate;

    /**
     * Resolved target item type. Inferred from the shuttle_ball catalog for
     * SHUTTLE_BALL, otherwise GOODS.
     */
    private InventoryItemType itemType;

    /**
     * ADD = create item then intake; UPDATE = intake onto existing item.
     * (SKIP/ERROR also possible for invalid rows.)
     */
    private ImportAction action;

    private String message;

    /**
     * Resolved existing item for UPDATE rows.
     */
    private Object itemRef;
}
