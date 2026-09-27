package com.badminton.service;

import com.badminton.response.product.ProductImportPreviewResponse;
import com.badminton.response.result.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;

/**
 * Import/export for the product catalog (shuttle balls + services).
 */
public interface ProductImportExportService {

    /**
     * Writes the active product catalog as an .xlsx workbook
     * (ShuttleBall + Service sheets). System pricing rows
     * (costInPerson, rentByTime) are never included.
     */
    void exportProducts(OutputStream outputStream);

    /**
     * Writes a headers-only .xlsx workbook matching the import format.
     */
    void exportTemplate(OutputStream outputStream);

    /**
     * Builds the export file name: products_YYYYMMDD_HHmmss.xlsx
     */
    String buildExportFileName();

    /**
     * Parses and validates the uploaded .xlsx, classifies each row
     * (ADD/UPDATE/REACTIVATE/SKIP/ERROR) and caches the result under a token.
     *
     * @param file uploaded .xlsx
     * @param mode MERGE | REPLACE (null/blank defaults to MERGE)
     */
    Result<ProductImportPreviewResponse> previewImport(MultipartFile file, String mode);

    /**
     * Applies a previously previewed import transactionally.
     *
     * @param importToken token returned by {@link #previewImport}
     * @param mode        optional override of the mode captured at preview time
     */
    Result<Boolean> commitImport(String importToken, String mode);
}
