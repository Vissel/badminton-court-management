package com.badminton.model.product;

import com.badminton.enums.ProductImportMode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Cached, classified import produced by the preview endpoint and applied by commit.
 */
@Data
@AllArgsConstructor
public class ProductImportPlan {

    private ProductImportMode mode;

    private List<ProductImportRow> rows;

    /**
     * Normalized (trimmed, lower-cased) product names present in the file, per sheet key.
     * Used by REPLACE mode to detect products missing from the file.
     */
    private Set<String> importedShuttleNames;

    private Set<String> importedServiceNames;

    /**
     * Creation time (epoch millis) for token expiry.
     */
    private long createdAt;
}
