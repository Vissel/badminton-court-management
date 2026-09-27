package com.badminton.response.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight stock lookup for the service/shuttle pickers: enough for the
 * frontend to refresh one option without reloading the catalog.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockCheckResponse {

    private int itemId;

    private String itemName;

    private String itemType;

    /**
     * Stock counted in base units.
     */
    private long stockOnHand;

    private boolean lowStock;

    private boolean outOfStock;
}
