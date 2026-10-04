package com.badminton.model.inventory;

import com.badminton.enums.InventoryItemType;
import com.badminton.enums.MovementReferenceType;
import com.badminton.enums.StockMovementType;
import lombok.Data;

@Data
public class ConsumedItemsModel {
    private String itemName;
    private InventoryItemType itemType;
    private StockMovementType consumeType;
    private int quantity;
    private MovementReferenceType referenceType;
    private Long referenceId;
    private int sign;
    private String note;
}
