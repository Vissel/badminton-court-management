package com.badminton.response.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response body of POST /api/products/import/preview.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImportPreviewResponse {

    /**
     * Token referencing the cached, classified import plan.
     * Passed to /api/products/import/commit.
     */
    private String importToken;

    private List<ProductImportRowResponse> rows;

    private ProductImportCounts counts;
}
