package com.badminton.controller;

import com.badminton.requestmodel.product.ProductImportCommitRequest;
import com.badminton.response.product.ProductImportPreviewResponse;
import com.badminton.response.result.Result;
import com.badminton.service.ProductImportExportService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Product catalog import/export (shuttle balls + services).
 * Restricted to the root user - the FE only hides the nav item,
 * which is bypassable.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ROOT_USER = "rootuser";
    private static final String TEMPLATE_FILE_NAME = "products_template.xlsx";

    @Autowired
    private ProductImportExportService productImportExportService;

    /**
     * Export the active product catalog as .xlsx (ShuttleBall + Service sheets).
     */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportProducts() {
        if (!isRootUser()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String fileName = productImportExportService.buildExportFileName();
        StreamingResponseBody stream = outputStream -> productImportExportService.exportProducts(outputStream);
        return xlsxResponse(stream, fileName);
    }

    /**
     * Headers-only .xlsx template matching the import format.
     */
    @GetMapping("/template")
    public ResponseEntity<StreamingResponseBody> downloadTemplate() {
        if (!isRootUser()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        StreamingResponseBody stream = outputStream -> productImportExportService.exportTemplate(outputStream);
        return xlsxResponse(stream, TEMPLATE_FILE_NAME);
    }

    /**
     * Parse + classify the uploaded file without writing to the DB.
     * Returns an importToken for {@link #commitImport}.
     */
    @PostMapping("/import/preview")
    public ResponseEntity<Result<ProductImportPreviewResponse>> previewImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mode", required = false) String mode) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(productImportExportService.previewImport(file, mode));
    }

    /**
     * Apply a cached preview transactionally.
     * Optional {@code mode=REPLACE} deactivates active products absent from the file.
     */
    @PostMapping("/import/commit")
    public ResponseEntity<Result<Boolean>> commitImport(
            @RequestBody ProductImportCommitRequest request,
            @RequestParam(value = "mode", required = false) String mode) {
        if (!isRootUser()) {
            return forbidden();
        }
        String token = request == null ? null : request.getImportToken();
        return ResponseConvertor.convert(productImportExportService.commitImport(token, mode));
    }

    private ResponseEntity<StreamingResponseBody> xlsxResponse(StreamingResponseBody stream, String fileName) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(stream);
    }

    private boolean isRootUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && ROOT_USER.equals(authentication.getName());
    }

    private <T> ResponseEntity<Result<T>> forbidden() {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.FORBIDDEN.value());
        result.setErrorMessage("Forbidden");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}
