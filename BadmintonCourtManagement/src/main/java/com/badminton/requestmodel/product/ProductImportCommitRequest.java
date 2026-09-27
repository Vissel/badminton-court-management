package com.badminton.requestmodel.product;

import lombok.Data;

/**
 * Request body of POST /api/products/import/commit.
 */
@Data
public class ProductImportCommitRequest {

    /**
     * Token returned by the preview endpoint.
     */
    private String importToken;
}
