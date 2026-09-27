package com.badminton.response.inventory;

import com.badminton.response.product.ProductImportCounts;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response body of POST /api/inventory/import/preview.
 * {@code importToken} is passed to /api/inventory/import/commit.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockIntakePreviewResponse {

    private String importToken;

    private List<StockIntakeRowResponse> rows;

    private ProductImportCounts counts;
}
