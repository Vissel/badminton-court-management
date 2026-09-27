package com.badminton.model.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Cached, classified stock-intake file produced by the preview endpoint
 * and applied by commit. Mirrors {@code ProductImportPlan}.
 */
@Data
@AllArgsConstructor
public class StockIntakePlan {

    private List<StockIntakeRow> rows;

    /**
     * Creation time (epoch millis) for token expiry.
     */
    private long createdAt;
}
