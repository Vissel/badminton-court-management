package com.badminton.controller;

import com.badminton.enums.InventoryItemType;
import com.badminton.requestmodel.inventory.AdjustmentRequest;
import com.badminton.requestmodel.inventory.InventoryItemRequest;
import com.badminton.requestmodel.inventory.PurchaseRequest;
import com.badminton.requestmodel.product.ProductImportCommitRequest;
import com.badminton.response.inventory.InventoryItemResponse;
import com.badminton.response.inventory.StockCheckResponse;
import com.badminton.response.inventory.StockIntakePreviewResponse;
import com.badminton.response.inventory.StockMovementResponse;
import com.badminton.response.result.Result;
import com.badminton.service.InventoryService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.List;

/**
 * Inventory management (stockable goods: shuttle balls, goods, stockable
 * services).
 * Restricted to the root user - same pattern as {@link ProductsController}.
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String ROOT_USER = "rootuser";

    @Autowired
    private InventoryService inventoryService;

    /**
     * Items with stock on hand, weighted avg cost, retail price, margin.
     */
    @GetMapping("/items")
    public ResponseEntity<Result<List<InventoryItemResponse>>> listItems(
            @RequestParam(value = "itemType", required = false) String itemType) {
        if (!isRootUser()) {
            return forbidden();
        }
        InventoryItemType type = null;
        if (itemType != null && !itemType.isBlank()) {
            try {
                type = InventoryItemType.valueOf(itemType.trim());
            } catch (IllegalArgumentException e) {
                Result<List<InventoryItemResponse>> error = new Result<>();
                error.setSuccess(false);
                error.setErrorCode(400);
                error.setErrorMessage("Invalid itemType: " + itemType);
                return ResponseConvertor.convert(error);
            }
        }
        return ResponseConvertor.convert(inventoryService.listItems(type));
    }

    /**
     * Lightweight stock check for the staff-facing pickers (HomePage service /
     * shuttle flows). Intentionally NOT rootuser-gated: it only exposes
     * stockOnHand/lowStock, the same data already served by
     * /court-mana/getServices to regular staff.
     */
    @GetMapping("/checkStock")
    public ResponseEntity<Result<StockCheckResponse>> checkStock(
            @RequestParam(value = "itemId", required = false) Integer itemId,
            @RequestParam(value = "item", required = false) String itemName,
            @RequestParam(value = "itemType", required = false) String itemType) {
        if (itemId == null && (itemName == null || itemName.isBlank())) {
            Result<StockCheckResponse> error = new Result<>();
            error.setSuccess(false);
            error.setErrorCode(400);
            error.setErrorMessage("itemId or item is required.");
            return ResponseConvertor.convert(error);
        }
        InventoryItemType type = null;
        if (itemType != null && !itemType.isBlank()) {
            try {
                type = InventoryItemType.valueOf(itemType.trim());
            } catch (IllegalArgumentException e) {
                Result<StockCheckResponse> error = new Result<>();
                error.setSuccess(false);
                error.setErrorCode(400);
                error.setErrorMessage("Invalid itemType: " + itemType);
                return ResponseConvertor.convert(error);
            }
        }
        return ResponseConvertor.convert(
                inventoryService.checkStock(itemId, itemName, type));
    }

    @PostMapping("/items")
    public ResponseEntity<Result<InventoryItemResponse>> createItem(
            @RequestBody InventoryItemRequest request) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(inventoryService.saveItem(null, request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<Result<InventoryItemResponse>> updateItem(
            @PathVariable("id") Integer itemId,
            @RequestBody InventoryItemRequest request) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(inventoryService.saveItem(itemId, request));
    }

    /**
     * Record a wholesale purchase (lot + PURCHASE_IN movement).
     */
    @PostMapping("/purchases")
    public ResponseEntity<Result<InventoryItemResponse>> recordPurchase(
            @RequestBody PurchaseRequest request) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(inventoryService.recordPurchase(request));
    }

    /**
     * Stock ledger for one item, optionally bounded by created_date.
     */
    @GetMapping("/movements")
    public ResponseEntity<Result<List<StockMovementResponse>>> listMovements(
            @RequestParam("itemId") Integer itemId,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(inventoryService.listMovements(itemId, from, to));
    }

    /**
     * Stock-take correction with mandatory reason.
     */
    @PostMapping("/adjustments")
    public ResponseEntity<Result<InventoryItemResponse>> adjustStock(
            @RequestBody AdjustmentRequest request) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(inventoryService.adjustStock(request));
    }

    /**
     * .xlsx report: stock on hand, avg cost, retail price, margin per item.
     */
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportInventory() {
        if (!isRootUser()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        StreamingResponseBody stream = outputStream -> inventoryService.exportInventory(outputStream);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + inventoryService.buildExportFileName() + "\"")
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(stream);
    }

    /**
     * Parse + classify a StockIntake workbook without writing to the DB.
     * Returns an importToken for {@link #commitIntake}.
     */
    @PostMapping("/import/preview")
    public ResponseEntity<Result<StockIntakePreviewResponse>> previewIntake(
            @RequestParam("file") MultipartFile file) {
        if (!isRootUser()) {
            return forbidden();
        }
        return ResponseConvertor.convert(inventoryService.previewIntake(file));
    }

    /**
     * Apply a cached intake plan transactionally.
     */
    @PostMapping("/import/commit")
    public ResponseEntity<Result<Boolean>> commitIntake(
            @RequestBody ProductImportCommitRequest request) {
        if (!isRootUser()) {
            return forbidden();
        }
        String token = request == null ? null : request.getImportToken();
        return ResponseConvertor.convert(inventoryService.commitIntake(token));
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
