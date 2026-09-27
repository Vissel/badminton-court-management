package com.badminton.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated counts per action of a product import preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportCounts {

    private int added;
    private int updated;
    private int reactivated;
    private int skipped;
    private int errors;
}
