package com.badminton.service;

import com.badminton.entity.Game;
import com.badminton.entity.InventoryItem;
import com.badminton.enums.InventoryItemType;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.requestmodel.inventory.AdjustmentRequest;
import com.badminton.requestmodel.inventory.InventoryItemRequest;
import com.badminton.requestmodel.inventory.PurchaseRequest;
import com.badminton.response.inventory.InventoryItemResponse;
import com.badminton.response.inventory.StockCheckResponse;
import com.badminton.response.inventory.StockIntakePreviewResponse;
import com.badminton.response.inventory.StockMovementResponse;
import com.badminton.response.result.Result;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

/**
 * Inventory management: stockable items, wholesale purchases, the stock
 * movement ledger, and the intake Excel import. All endpoints are
 * rootuser-gated.
 */
public interface InventoryService {

    /**
     * Items with computed stock on hand, weighted avg cost and margin.
     * itemType null = all types.
     */
    Result<List<InventoryItemResponse>> listItems(InventoryItemType itemType);

    /**
     * Lightweight stock lookup for picker refresh. Resolved by itemId first,
     * otherwise by (itemName, itemType).
     */
    Result<StockCheckResponse> checkStock(Integer itemId, String itemName,
            InventoryItemType itemType);

    /**
     * Creates an item (itemId null) or updates name/unit/retailPrice/isActive.
     */
    Result<InventoryItemResponse> saveItem(Integer itemId, InventoryItemRequest request);

    /**
     * Records a wholesale purchase: purchase_lot row + PURCHASE_IN movement,
     * transactionally.
     */
    Result<InventoryItemResponse> recordPurchase(PurchaseRequest request);

    Result<List<StockMovementResponse>> listMovements(Integer itemId, LocalDate from, LocalDate to);

    /**
     * Stock-take correction: ADJUSTMENT movement, reason mandatory.
     */
    Result<InventoryItemResponse> adjustStock(AdjustmentRequest request);

    /**
     * Writes the inventory report (.xlsx): stock, avg cost, retail price, margin.
     */
    void exportInventory(OutputStream outputStream);

    /**
     * Builds the export file name: inventory_YYYYMMDD_HHmmss.xlsx
     */
    String buildExportFileName();

    /**
     * Parses the StockIntake sheet (Tên | Số lượng | Giá nhập | Ngày nhập),
     * classifies each row (ADD/UPDATE/REACTIVATE/ERROR) and caches the plan
     * under a token. No DB writes.
     */
    Result<StockIntakePreviewResponse> previewIntake(MultipartFile file);

    /**
     * Applies a previously previewed intake transactionally.
     */
    Result<Boolean> commitIntake(String importToken);

    // ------------------------------------------------------------------
    // Internal stock-deduction hooks (called by game/payment flows)
    // ------------------------------------------------------------------

    /**
     * Writes GAME_CONSUMPTION movements for every shuttle ball mapped on the game.
     * Call once the game is finished.
     */
    void recordGameConsumption(Game game);

    /**
     * Logs low-stock warnings for the shuttle balls selected on a starting game.
     * Non-blocking.
     */
    void warnLowStockForGame(Game game);

    /**
     * Writes RETAIL_SALE movements for service lines carrying an itemId.
     * Called when a service is added to a player.
     */
    void recordRetailSales(List<ServiceDTO> services, Long refId);

    /**
     * Writes RETURN movements when a previously-added service line is removed
     * before payment (service delete, update downgrade, player/session removal).
     */
    void recordServiceReturns(List<ServiceDTO> services, Long refId);

    /**
     * Current stock on hand in base units for the given item.
     */
    long getStockOnHand(InventoryItem item);
}
