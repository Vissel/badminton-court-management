package com.badminton.service.impl;

import com.badminton.core.inventory.CoreInventoryService;
import com.badminton.entity.*;
import com.badminton.enums.ImportAction;
import com.badminton.enums.InventoryItemType;
import com.badminton.enums.MovementReferenceType;
import com.badminton.enums.StockMovementType;
import com.badminton.exception.BusinessException;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.model.inventory.StockIntakePlan;
import com.badminton.model.inventory.StockIntakeRow;
import com.badminton.repository.*;
import com.badminton.requestmodel.inventory.AdjustmentRequest;
import com.badminton.requestmodel.inventory.BallConsumeInGameRequest;
import com.badminton.requestmodel.inventory.InventoryItemRequest;
import com.badminton.requestmodel.inventory.PurchaseRequest;
import com.badminton.response.inventory.*;
import com.badminton.response.product.ProductImportCounts;
import com.badminton.response.result.Result;
import com.badminton.service.InventoryService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import com.badminton.service.product.StockIntakeParser;
import com.badminton.service.report.InventoryExcelWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Inventory management backed by the {@code stock_movement} ledger.
 * Stock on hand = SUM(quantity_delta); weighted average cost is replayed
 * over the ledger (moving-average on each PURCHASE_IN while qty > 0).
 */
@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    private static final String XLSX_EXTENSION = ".xlsx";
    private static final long IMPORT_TOKEN_TTL_MS = 30L * 60 * 1000;
    private static final int LOW_STOCK_THRESHOLD = 10;
    private static final String EXPORT_FILE_PREFIX = "inventory_";
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Purchase-unit kinds accepted in {@code PurchaseRequest.unit} / intake Đơn vị.
     */
    private static final String UNIT_BASE = "BASE";
    private static final String UNIT_PACKAGE = "PACKAGE";
    /**
     * Default unit config auto-applied to new SHUTTLE_BALL items.
     */
    private static final String BALL_BASE_UNIT = "quả";
    private static final String BALL_PACKAGE_UNIT = "ống";
    private static final int BALL_UNITS_PER_PACKAGE = 12;

    /**
     * Parsed intake plans awaiting commit. Same in-memory cache pattern
     * as {@code ProductImportExportServiceImpl.importCache}.
     */
    private final Map<String, StockIntakePlan> intakeCache = new ConcurrentHashMap<>();

    @Autowired
    private InventoryItemRepository itemRepo;

    @Autowired
    private PurchaseLotRepository lotRepo;

    @Autowired
    private StockMovementRepository movementRepo;

    @Autowired
    private ShuttleBallRepository shuttleRepo;

    @Autowired
    private ServiceRepository serviceRepo;


    @Autowired
    private StockIntakeParser stockIntakeParser;

    @Autowired
    private InventoryExcelWriter inventoryExcelWriter;

    @Autowired
    private ServiceTemplate serviceTemplate;

    @Autowired
    private CoreInventoryService coreInventoryService;

    // ------------------------------------------------------------------
    // items
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Result<List<InventoryItemResponse>> listItems(InventoryItemType itemType) {
        Result<List<InventoryItemResponse>> result = new Result<>();
        try {
            List<InventoryItem> entities = itemType != null
                    ? itemRepo.findAllByIsActiveAndItemType(true, itemType)
                    : itemRepo.findAllByIsActive(true);
            List<InventoryItemResponse> items = entities.stream()
                    .map(this::toResponse)
                    .toList();
            result.setSuccess(true);
            result.setData(items);
        } catch (RuntimeException e) {
            log.error("Failed to list inventory items", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<StockCheckResponse> checkStock(Integer itemId, String itemName,
                                                 InventoryItemType itemType) {
        Result<StockCheckResponse> result = new Result<>();
        try {
            InventoryItem item = null;
            if (itemId != null) {
                item = itemRepo.findById(itemId).orElse(null);
            }
            if (item == null && itemName != null && itemType != null) {
                item = itemRepo.findByItemNameIgnoreCaseAndItemType(
                        itemName.trim(), itemType).orElse(null);
            }
            if (item == null || !item.isActive()) {
                result.setSuccess(false);
                result.setErrorCode(404);
                result.setErrorMessage("Item not found.");
                return result;
            }
            long stockOnHand = movementRepo.sumQuantityDeltaByItem(item);
            StockCheckResponse data = new StockCheckResponse(item.getItemId(),
                    item.getItemName(),
                    item.getItemType() != null ? item.getItemType().name() : null,
                    stockOnHand,
                    stockOnHand <= LOW_STOCK_THRESHOLD,
                    stockOnHand <= 0);
            result.setSuccess(true);
            result.setData(data);
        } catch (RuntimeException e) {
            log.error("Failed to check stock", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    @Override
    @Transactional
    public Result<InventoryItemResponse> saveItem(Integer itemId, InventoryItemRequest request) {
        Result<InventoryItemResponse> result = new Result<>();
        try {
            validateItemRequest(request, itemId == null);
            InventoryItemType type = parseItemType(request.getItemType(), itemId == null);

            InventoryItem item;
            if (itemId == null) {
                item = new InventoryItem(request.getItemName().trim(), type);
                applyUnitDefaults(item);
                if (request.getUnit() != null && !request.getUnit().isBlank()) {
                    item.setUnit(request.getUnit().trim());
                }
                applyPackageRequest(item, request);
                item.setRetailPrice(request.getRetailPrice());
            } else {
                item = itemRepo.findById(itemId)
                        .orElseThrow(() -> new IllegalArgumentException("Item không tồn tại: " + itemId));
                if (request.getItemName() != null && !request.getItemName().isBlank()) {
                    item.setItemName(request.getItemName().trim());
                }
                if (request.getUnit() != null) {
                    item.setUnit(request.getUnit());
                }
                applyPackageRequest(item, request);
                if (request.getRetailPrice() != null) {
                    item.setRetailPrice(request.getRetailPrice());
                }
                if (request.getIsActive() != null) {
                    item.setActive(request.getIsActive());
                }
            }
            validatePackageConfig(item);
            result.setSuccess(true);
            result.setData(toResponse(itemRepo.save(item)));
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to save inventory item", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    // ------------------------------------------------------------------
    // purchases / adjustments / movements
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Result<InventoryItemResponse> recordPurchase(PurchaseRequest request) {
        Result<InventoryItemResponse> result = new Result<>();
        try {
            validatePurchase(request);
            InventoryItem item = resolveOrCreateItem(request);
            UnitSelection selection = resolveUnitSelection(item, request.getUnit());
            int baseQty = request.getQuantity() * selection.multiplier();

            PurchaseLot lot = new PurchaseLot();
            lot.setItem(item);
            lot.setPurchaseDate(request.getPurchaseDate() != null ? request.getPurchaseDate() : LocalDate.now());
            lot.setQuantity(request.getQuantity());
            lot.setUnit(selection.label());
            lot.setBaseQuantity(baseQty);
            lot.setUnitCost(request.getUnitCost());
            lot.setTotalCost(request.getUnitCost().multiply(BigDecimal.valueOf(request.getQuantity())));
            lot.setSupplier(request.getSupplier());
            lot.setNote(request.getNote());
            PurchaseLot savedLot = lotRepo.save(lot);

            StockMovement movement = baseMovement(item, StockMovementType.PURCHASE_IN,
                    baseQty, request.getNote());
            movement.setUnitCost(baseUnitCost(request.getUnitCost(), selection.multiplier()));
            movement.setRefType(MovementReferenceType.PURCHASE_LOT);
            movement.setRefId(savedLot.getLotId());
            movementRepo.save(movement);

            result.setSuccess(true);
            result.setData(toResponse(item));
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to record purchase", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Nhập kho thất bại, toàn bộ thay đổi đã được hủy");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<List<StockMovementResponse>> listMovements(Integer itemId, LocalDate from, LocalDate to) {
        Result<List<StockMovementResponse>> result = new Result<>();
        try {
            InventoryItem item = requireItem(itemId);
            List<StockMovement> movements;
            if (from != null || to != null) {
                ZoneId zone = ZoneId.systemDefault();
                Timestamp fromTs = Timestamp.from(
                        (from != null ? from : LocalDate.of(1970, 1, 1)).atStartOfDay(zone).toInstant());
                Timestamp toTs = Timestamp.from(
                        (to != null ? to.plusDays(1) : LocalDate.now().plusDays(1)).atStartOfDay(zone).toInstant());
                movements = movementRepo.findAllByItemAndCreatedDateBetweenOrderByMovementIdAsc(item, fromTs, toTs);
            } else {
                movements = movementRepo.findAllByItemOrderByMovementIdAsc(item);
            }
            result.setSuccess(true);
            result.setData(movements.stream().map(this::toMovementResponse).toList());
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to list stock movements", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    @Override
    @Transactional
    public Result<InventoryItemResponse> adjustStock(AdjustmentRequest request) {
        Result<InventoryItemResponse> result = new Result<>();
        try {
            if (request.getItemId() == null) {
                throw new IllegalArgumentException("itemId is required");
            }
            if (request.getQuantityDelta() == null || request.getQuantityDelta() == 0) {
                throw new IllegalArgumentException("quantityDelta phải khác 0");
            }
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new IllegalArgumentException("Lý do điều chỉnh là bắt buộc");
            }
            InventoryItem item = requireItem(request.getItemId());
            movementRepo.save(baseMovement(item, StockMovementType.ADJUSTMENT,
                    request.getQuantityDelta(), request.getReason().trim()));
            result.setSuccess(true);
            result.setData(toResponse(item));
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Failed to adjust stock", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    // ------------------------------------------------------------------
    // export
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public void exportInventory(OutputStream outputStream) {
        List<InventoryItemResponse> items = itemRepo.findAllByIsActive(true).stream()
                .map(this::toResponse)
                .toList();
        inventoryExcelWriter.write(items, outputStream);
    }

    public String buildExportFileName() {
        return EXPORT_FILE_PREFIX + LocalDateTime_now() + XLSX_EXTENSION;
    }

    private String LocalDateTime_now() {
        return java.time.LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }

    // ------------------------------------------------------------------
    // stock intake import (preview / commit - same pattern as product import)
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Result<StockIntakePreviewResponse> previewIntake(MultipartFile file) {
        Result<StockIntakePreviewResponse> result = new Result<>();
        try {
            validateFile(file);
            List<StockIntakeRow> rows;
            try (var inputStream = file.getInputStream()) {
                rows = stockIntakeParser.parse(inputStream);
            } catch (IOException e) {
                throw new IllegalArgumentException("Không đọc được file Excel");
            }
            classify(rows);

            String token = UUID.randomUUID().toString();
            intakeCache.put(token, new StockIntakePlan(rows, System.currentTimeMillis()));

            result.setSuccess(true);
            result.setData(new StockIntakePreviewResponse(token, toRowResponses(rows), toCounts(rows)));
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Stock intake preview failed", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Server error.");
        }
        return result;
    }

    @Override
    @Transactional
    public Result<Boolean> commitIntake(String importToken) {
        Result<Boolean> result = new Result<>();
        try {
            if (importToken == null || importToken.isBlank()) {
                throw new IllegalArgumentException("importToken is required");
            }
            // remove() as lookup: strictly single-use even under concurrent commits
            StockIntakePlan plan = intakeCache.remove(importToken);
            if (plan == null) {
                throw new IllegalArgumentException("Import token không hợp lệ hoặc đã được sử dụng");
            }
            if (System.currentTimeMillis() - plan.getCreatedAt() > IMPORT_TOKEN_TTL_MS) {
                throw new IllegalArgumentException("Import token đã hết hạn, vui lòng tải file lên lại");
            }
            applyIntake(plan);
            result.setSuccess(true);
            result.setData(Boolean.TRUE);
        } catch (IllegalArgumentException e) {
            result.setSuccess(false);
            result.setErrorCode(400);
            result.setErrorMessage(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Stock intake commit failed", e);
            result.setSuccess(false);
            result.setErrorCode(500);
            result.setErrorMessage("Nhập kho thất bại, toàn bộ thay đổi đã được hủy");
        }
        return result;
    }

    private void classify(List<StockIntakeRow> rows) {
        for (StockIntakeRow row : rows) {
            if (row.getAction() == ImportAction.ERROR) {
                continue;
            }
            classifyRow(row);
        }
    }

    private void classifyRow(StockIntakeRow row) {
        InventoryItem shuttleItem = itemRepo
                .findByItemNameIgnoreCaseAndItemType(row.getName(), InventoryItemType.SHUTTLE_BALL)
                .orElse(null);
        InventoryItem goodsItem = itemRepo
                .findByItemNameIgnoreCaseAndItemType(row.getName(), InventoryItemType.GOODS)
                .orElse(null);
        InventoryItem item = shuttleItem != null ? shuttleItem : goodsItem;

        if (item == null) {
            // infer type from the shuttle_ball catalog for new items
            boolean isBall = shuttleRepo.findAllByShuttleName(row.getName()).stream()
                    .anyMatch(ShuttleBall::isActive);
            row.setItemType(isBall ? InventoryItemType.SHUTTLE_BALL : InventoryItemType.GOODS);
            row.setAction(ImportAction.ADD);
            row.setMessage("Tạo mới sản phẩm + nhập kho");
        } else {
            row.setItemType(item.getItemType());
            row.setItemRef(item);
            if (item.isActive()) {
                row.setAction(ImportAction.UPDATE);
                row.setMessage("Nhập thêm vào sản phẩm hiện có");
            } else {
                row.setAction(ImportAction.REACTIVATE);
                row.setMessage("Kích hoạt lại + nhập kho");
            }
        }
        applyUnitResolution(row, item);
    }

    /**
     * Resolves the raw Đơn vị text into a purchase unit + base quantity.
     * Invalid input flips the row to ERROR so the commit loop skips it.
     */
    private void applyUnitResolution(StockIntakeRow row, InventoryItem item) {
        try {
            UnitSelection selection = resolveUnitSelection(
                    item != null ? item : newItemWithUnitDefaults(row.getItemType()),
                    row.getUnit());
            row.setUnit(selection.label());
            row.setBaseQuantity(row.getQuantity() * selection.multiplier());
        } catch (IllegalArgumentException e) {
            row.setAction(ImportAction.ERROR);
            row.setMessage(e.getMessage());
        }
    }

    private void applyIntake(StockIntakePlan plan) {
        for (StockIntakeRow row : plan.getRows()) {
            if (row.getAction() == null
                    || row.getAction() == ImportAction.ERROR
                    || row.getAction() == ImportAction.SKIP) {
                continue;
            }
            InventoryItem item = resolveIntakeItem(row);
            // a new GOODS item can adopt the raw Đơn vị text as its base unit
            if (item.getUnit() == null && row.getUnit() != null) {
                item.setUnit(row.getUnit());
                itemRepo.save(item);
            }
            int baseQty = row.getBaseQuantity() != null ? row.getBaseQuantity() : row.getQuantity();

            PurchaseLot lot = new PurchaseLot();
            lot.setItem(item);
            lot.setPurchaseDate(row.getPurchaseDate() != null ? row.getPurchaseDate() : LocalDate.now());
            lot.setQuantity(row.getQuantity());
            lot.setUnit(row.getUnit());
            lot.setBaseQuantity(baseQty);
            lot.setUnitCost(row.getUnitCost());
            lot.setTotalCost(row.getUnitCost().multiply(BigDecimal.valueOf(row.getQuantity())));
            lot.setNote("Import file");
            PurchaseLot savedLot = lotRepo.save(lot);

            StockMovement movement = baseMovement(item, StockMovementType.PURCHASE_IN,
                    baseQty, "Import file");
            movement.setUnitCost(baseUnitCost(row.getUnitCost(),
                    row.getQuantity() > 0 ? baseQty / row.getQuantity() : 1));
            movement.setRefType(MovementReferenceType.PURCHASE_LOT);
            movement.setRefId(savedLot.getLotId());
            movementRepo.save(movement);
        }
    }

    private InventoryItem resolveIntakeItem(StockIntakeRow row) {
        if (row.getItemRef() instanceof InventoryItem item) {
            // reload - the ref was loaded in a read-only preview transaction
            InventoryItem managed = itemRepo.findById(item.getItemId())
                    .orElseThrow(() -> new IllegalStateException("Item đã bị xóa: " + row.getName()));
            if (!managed.isActive()) {
                managed.setActive(true);
                itemRepo.save(managed);
            }
            return managed;
        }
        // ADD - item may have been created between preview and commit
        return itemRepo.findByItemNameIgnoreCaseAndItemType(row.getName(), row.getItemType())
                .orElseGet(() -> {
                    InventoryItem created = new InventoryItem(row.getName(), row.getItemType());
                    applyUnitDefaults(created);
                    return itemRepo.save(created);
                });
    }

    // ------------------------------------------------------------------
    // game / retail hooks
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public Result<Boolean> recordBallConsumption(BallConsumeInGameRequest ballConsumeInGameRequest) {
        return serviceTemplate.execute(new ProcessCallback<BallConsumeInGameRequest, Boolean>() {
            @Override
            public BallConsumeInGameRequest getRequest() {
                return ballConsumeInGameRequest;
            }

            @Override
            public void preProcess(BallConsumeInGameRequest request) {
                Assert.notNull(request.getShuttleBallDTOList(), "Shuttle balls must not null");
                Assert.isTrue(request.getInGame() > 0, "Game is not exist.");
            }

            @Override
            public Boolean process() throws BusinessException {
                if (getRequest().getShuttleBallDTOList().isEmpty()) {
                    return false;
                }
                coreInventoryService.recordItemConsumption(toConsumptionMap(getRequest()), null);
                return true;
            }
        });
    }

    @Override
    @Transactional
    public Result<Boolean> revokeBallConsumption(List<ShuttleBallDTO> balls) {
        return serviceTemplate.execute(new ProcessCallback<List<ShuttleBallDTO>, Boolean>() {
            @Override
            public List<ShuttleBallDTO> getRequest() {
                return balls;
            }

            @Override
            public void preProcess(List<ShuttleBallDTO> request) {
                Assert.notEmpty(balls, "Shuttle balls must not empty");
            }

            @Override
            public Boolean process() throws BusinessException {
                coreInventoryService.revokeItemConsumption(toConsumptionMap(balls), null);
                return true;
            }
        });
    }

    private Map<InventoryItem, Integer> toConsumptionMap(BallConsumeInGameRequest request) {
        return toConsumptionMap(request.getShuttleBallDTOList());
    }

    private Map<InventoryItem, Integer> toConsumptionMap(List<ShuttleBallDTO> balls) {
        Map<InventoryItem, Integer> items = new LinkedHashMap<>();
        for (ShuttleBallDTO ball : balls) {
            InventoryItem item = resolveItemForBallDTO(ball);
            if (item != null) {
                items.merge(item, ball.getBallQuantity(), Integer::sum);
            }
        }
        return items;
    }

    private InventoryItem resolveItemForBallDTO(ShuttleBallDTO ballDTO) {
        ShuttleBall ball = shuttleRepo.findAllByShuttleName(ballDTO.getShuttleName()).stream()
                .filter(ShuttleBall::isActive).findFirst().orElse(null);
        if (ball != null) {
            return resolveItemForBall(ball);
        }
        return itemRepo.findByItemNameIgnoreCaseAndItemType(
                ballDTO.getShuttleName(), InventoryItemType.SHUTTLE_BALL).orElse(null);
    }

    /**
     * Resolves the stock item for a catalog ball. Catalog rows created after
     * the initial backfill (AdminServiceImpl, product import) can carry
     * {@code item_id = NULL} - heal the link lazily so stock always deducts.
     */
    private InventoryItem resolveItemForBall(ShuttleBall ball) {
        if (ball.getItem() != null) {
            return ball.getItem();
        }
        InventoryItem item = itemRepo
                .findByItemNameIgnoreCaseAndItemType(ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL)
                .orElseGet(() -> {
                    InventoryItem created = new InventoryItem(ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL);
                    applyUnitDefaults(created);
                    return itemRepo.save(created);
                });
        ball.setItem(item);
        shuttleRepo.save(ball);
        return item;
    }

    /**
     * look meaningless
     *
     * @param game
     */
    @Override
    @Transactional(readOnly = true)
    public void warnLowStockForGame(Game game) {
        if (game == null || game.getShuttleMap() == null) {
            return;
        }
        for (GameShuttleMap map : game.getShuttleMap()) {
            ShuttleBall ball = map.getShuttleBall();
            if (ball == null) {
                continue;
            }
            InventoryItem item = ball.getItem() != null ? ball.getItem()
                    : itemRepo.findByItemNameIgnoreCaseAndItemType(
                    ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL).orElse(null);
            if (item == null) {
                continue;
            }
            long stock = movementRepo.sumQuantityDeltaByItem(item);
            if (stock < map.getShuttleNumber()) {
                log.warn("Low stock for '{}': need {}, have {}", ball.getShuttleName(),
                        map.getShuttleNumber(), stock);
            }
        }
    }

    @Override
    @Transactional
    public void recordRetailSales(List<ServiceDTO> services, Long refId) {
        coreInventoryService.retailItem(services, refId);
    }

    @Override
    @Transactional
    public void recordServiceReturns(List<ServiceDTO> services, Long refId) {
        recordServiceMovements(services, refId, StockMovementType.RETURN, 1);
    }

    private void recordServiceMovements(List<ServiceDTO> services, Long refId,
                                        StockMovementType type, int sign) {
        if (services == null) {
            return;
        }
        for (ServiceDTO line : services) {
            int qty = line.getQuantity() != null && line.getQuantity() > 0 ? line.getQuantity() : 1;
            InventoryItem item = resolveServiceItem(line);
            if (item == null) {
                continue;
            }
            StockMovement movement = baseMovement(item, type, sign * qty, line.getServiceName());
            movement.setRefType(MovementReferenceType.PAYMENT);
            movement.setRefId(refId);
            movementRepo.save(movement);
        }
    }

    private InventoryItem resolveServiceItem(ServiceDTO line) {
        if (line.getItemId() != null) {
            InventoryItem item = itemRepo.findById(line.getItemId()).orElse(null);
            if (item != null) {
                return item;
            }
        }
        if (line.getServiceName() == null || line.getServiceName().isBlank()) {
            return null;
        }
        com.badminton.entity.Service service = serviceRepo.findBySerName(line.getServiceName()).orElse(null);
        if (service != null && service.getItem() != null) {
            return service.getItem();
        }
        log.warn("Retail/return service skipped: no stock item for '{}'", line.getServiceName());
        return null;
    }

    @Override
    @Transactional
    public long getStockOnHand(InventoryItem item) {
        return movementRepo.sumQuantityDeltaByItem(item);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private InventoryItemResponse toResponse(InventoryItem item) {
        long stockOnHand = movementRepo.sumQuantityDeltaByItem(item);
        BigDecimal avgCost = computeWeightedAvgCost(item);
        BigDecimal retailPrice = resolveRetailPrice(item);
        BigDecimal margin = retailPrice != null && avgCost != null
                ? retailPrice.subtract(avgCost)
                : null;
        LocalDate lastPurchaseDate = lotRepo
                .findFirstByItemOrderByPurchaseDateDescLotIdDesc(item)
                .map(PurchaseLot::getPurchaseDate)
                .orElse(null);
        long purchasedQty = movementRepo.sumQuantityDeltaByItemAndMovementType(item, StockMovementType.PURCHASE_IN);
        InventoryItemResponse response = new InventoryItemResponse(item.getItemId(),
                item.getItemName(),
                item.getItemType() != null ? item.getItemType().name() : null,
                item.getUnit(), retailPrice, item.isActive(),
                item.getPackageUnit(), item.getUnitsPerPackage(),
                stockOnHand, null, avgCost, margin, stockOnHand <= LOW_STOCK_THRESHOLD,
                stockOnHand <= 0, lastPurchaseDate, purchasedQty, null);
        String breakdown = response.getPackageBreakdown();
        response.setStockLabel(breakdown != null
                ? stockOnHand + " " + unitLabel(item) + " (" + breakdown + ")"
                : stockOnHand + " " + unitLabel(item));
        response.setPurchasedBreakdown(response.getPackageBreakdownFor(purchasedQty));
        return response;
    }

    private String unitLabel(InventoryItem item) {
        return item.getUnit() != null ? item.getUnit() : "";
    }

    /**
     * Item's own retail price, else the latest active catalog price.
     */
    private BigDecimal resolveRetailPrice(InventoryItem item) {
        if (item.getRetailPrice() != null) {
            return item.getRetailPrice();
        }
        if (item.getItemType() == InventoryItemType.SHUTTLE_BALL) {
            return shuttleRepo.findAllByShuttleName(item.getItemName()).stream()
                    .filter(ShuttleBall::isActive)
                    .map(b -> BigDecimal.valueOf(b.getCost()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    /**
     * Weighted average cost of remaining stock: replay the ledger, carrying a
     * moving average that updates on each PURCHASE_IN while qty stays positive.
     */
    private BigDecimal computeWeightedAvgCost(InventoryItem item) {
        long qty = 0;
        BigDecimal avgCost = null;
        for (StockMovement m : movementRepo.findAllByItemOrderByMovementIdAsc(item)) {
            if (m.getMovementType() == StockMovementType.PURCHASE_IN
                    && m.getUnitCost() != null && m.getQuantityDelta() > 0) {
                if (qty <= 0) {
                    qty = m.getQuantityDelta();
                    avgCost = m.getUnitCost();
                } else {
                    BigDecimal total = avgCost.multiply(BigDecimal.valueOf(qty))
                            .add(m.getUnitCost().multiply(BigDecimal.valueOf(m.getQuantityDelta())));
                    qty += m.getQuantityDelta();
                    avgCost = total.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_UP);
                }
            } else {
                qty += m.getQuantityDelta();
            }
        }
        return avgCost;
    }

    private StockMovementResponse toMovementResponse(StockMovement m) {
        return new StockMovementResponse(m.getMovementId(),
                m.getItem().getItemId(), m.getItem().getItemName(),
                m.getMovementType().name(), m.getQuantityDelta(), m.getUnitCost(),
                m.getRefType() != null ? m.getRefType().name() : null,
                m.getRefId(), m.getNote(), m.getCreatedDate());
    }

    private StockMovement baseMovement(InventoryItem item, StockMovementType type,
                                       int delta, String note) {
        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setMovementType(type);
        movement.setQuantityDelta(delta);
        movement.setNote(note);
        return movement;
    }

    private InventoryItem resolveOrCreateItem(PurchaseRequest request) {
        if (request.getItemId() != null) {
            return requireItem(request.getItemId());
        }
        InventoryItemType type = parseItemType(request.getItemType(), true);
        String name = request.getItemName().trim();
        return itemRepo.findByItemNameIgnoreCaseAndItemType(name, type)
                .map(item -> {
                    if (!item.isActive()) {
                        item.setActive(true);
                        itemRepo.save(item);
                    }
                    return item;
                })
                .orElseGet(() -> {
                    InventoryItem created = new InventoryItem(name, type);
                    applyUnitDefaults(created);
                    return itemRepo.save(created);
                });
    }

    private InventoryItem requireItem(Integer itemId) {
        return itemRepo.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item không tồn tại: " + itemId));
    }

    private void validateItemRequest(InventoryItemRequest request, boolean create) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (create) {
            if (request.getItemName() == null || request.getItemName().isBlank()) {
                throw new IllegalArgumentException("Tên sản phẩm trống");
            }
        }
        if (request.getRetailPrice() != null && request.getRetailPrice().signum() < 0) {
            throw new IllegalArgumentException("Giá bán không được âm");
        }
        if (request.getUnitsPerPackage() != null && request.getUnitsPerPackage() <= 0) {
            throw new IllegalArgumentException("Quy đổi (số đơn vị/" + "ống) phải lớn hơn 0");
        }
    }


    /**
     * Default units: SHUTTLE_BALL → quả base / ống package of 12; GOODS keep
     * whatever is supplied (usually none → base-only intake).
     */
    private void applyUnitDefaults(InventoryItem item) {
        if (item.getItemType() == InventoryItemType.SHUTTLE_BALL) {
            item.setUnit(BALL_BASE_UNIT);
            item.setPackageUnit(BALL_PACKAGE_UNIT);
            item.setUnitsPerPackage(BALL_UNITS_PER_PACKAGE);
        }
    }

    private InventoryItem newItemWithUnitDefaults(InventoryItemType type) {
        InventoryItem item = new InventoryItem();
        item.setItemType(type);
        applyUnitDefaults(item);
        return item;
    }

    /**
     * Applies packageUnit/unitsPerPackage from the request. An explicitly blank
     * packageUnit clears the package config (both fields).
     */
    private void applyPackageRequest(InventoryItem item, InventoryItemRequest request) {
        if (request.getPackageUnit() != null) {
            String pkg = request.getPackageUnit().trim();
            if (pkg.isEmpty()) {
                item.setPackageUnit(null);
                item.setUnitsPerPackage(null);
            } else {
                item.setPackageUnit(pkg);
            }
        }
        if (request.getUnitsPerPackage() != null) {
            item.setUnitsPerPackage(request.getUnitsPerPackage());
        }
    }

    private void validatePackageConfig(InventoryItem item) {
        if (item.getPackageUnit() != null
                && (item.getUnitsPerPackage() == null || item.getUnitsPerPackage() <= 0)) {
            throw new IllegalArgumentException(
                    "Đã đặt đơn vị quy đổi (" + item.getPackageUnit()
                            + ") nhưng thiếu số lượng quy đổi");
        }
    }

    /**
     * Resolves the purchase-unit input ("BASE"/"PACKAGE" keywords or a literal
     * unit label like ống/quả) against the item's unit config.
     * Blank input defaults to the package unit when configured - wholesale
     * intake is usually done by package.
     */
    private UnitSelection resolveUnitSelection(InventoryItem item, String raw) {
        String baseLabel = item.getUnit();
        String packageLabel = item.getPackageUnit();
        int perPackage = item.getUnitsPerPackage() != null ? item.getUnitsPerPackage() : 0;

        if (raw == null || raw.isBlank()) {
            return packageLabel != null && perPackage > 0
                    ? new UnitSelection(packageLabel, perPackage)
                    : new UnitSelection(baseLabel, 1);
        }
        String text = raw.trim();
        if (UNIT_PACKAGE.equalsIgnoreCase(text)) {
            if (packageLabel == null || perPackage <= 0) {
                throw new IllegalArgumentException(
                        "Mặt hàng '" + item.getItemName() + "' chưa cấu hình đơn vị quy đổi");
            }
            return new UnitSelection(packageLabel, perPackage);
        }
        if (UNIT_BASE.equalsIgnoreCase(text)) {
            return new UnitSelection(baseLabel, 1);
        }
        if (packageLabel != null && packageLabel.equalsIgnoreCase(text)) {
            if (perPackage <= 0) {
                throw new IllegalArgumentException(
                        "Mặt hàng '" + item.getItemName() + "' thiếu số lượng quy đổi");
            }
            return new UnitSelection(packageLabel, perPackage);
        }
        if (baseLabel == null || baseLabel.isBlank() || baseLabel.equalsIgnoreCase(text)) {
            return new UnitSelection(text, 1);
        }
        throw new IllegalArgumentException("Đơn vị không hợp lệ: " + raw);
    }

    /**
     * Cost per base unit (ledger unit cost).
     */
    private BigDecimal baseUnitCost(BigDecimal purchasedUnitCost, int multiplier) {
        if (multiplier <= 1) {
            return purchasedUnitCost;
        }
        return purchasedUnitCost.divide(BigDecimal.valueOf(multiplier), 4, RoundingMode.HALF_UP);
    }

    private record UnitSelection(String label, int multiplier) {
    }

    private void validatePurchase(PurchaseRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getItemId() == null
                && (request.getItemName() == null || request.getItemName().isBlank())) {
            throw new IllegalArgumentException("itemId hoặc itemName là bắt buộc");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
        }
        if (request.getUnitCost() == null || request.getUnitCost().signum() < 0) {
            throw new IllegalArgumentException("Giá nhập không hợp lệ");
        }
    }

    private InventoryItemType parseItemType(String raw, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) {
                throw new IllegalArgumentException(
                        "itemType không hợp lệ. Giá trị cho phép: SHUTTLE_BALL, GOODS");
            }
            return null;
        }
        try {
            return InventoryItemType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "itemType không hợp lệ: " + raw + ". Giá trị cho phép: SHUTTLE_BALL, GOODS");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import trống");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(XLSX_EXTENSION)) {
            throw new IllegalArgumentException("Chỉ hỗ trợ file .xlsx");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File vượt quá 5MB");
        }
    }

    private List<StockIntakeRowResponse> toRowResponses(List<StockIntakeRow> rows) {
        List<StockIntakeRowResponse> responses = new ArrayList<>(rows.size());
        for (StockIntakeRow row : rows) {
            responses.add(new StockIntakeRowResponse(row.getRowNumber(), row.getName(),
                    row.getQuantity(), row.getUnit(), row.getBaseQuantity(),
                    row.getUnitCost(), row.getPurchaseDate(),
                    row.getItemType() != null ? row.getItemType().name() : null,
                    row.getAction() == null ? null : row.getAction().name(),
                    row.getMessage()));
        }
        return responses;
    }

    private ProductImportCounts toCounts(List<StockIntakeRow> rows) {
        ProductImportCounts counts = new ProductImportCounts();
        for (StockIntakeRow row : rows) {
            if (row.getAction() == null) {
                continue;
            }
            switch (row.getAction()) {
                case ADD -> counts.setAdded(counts.getAdded() + 1);
                case UPDATE -> counts.setUpdated(counts.getUpdated() + 1);
                case REACTIVATE -> counts.setReactivated(counts.getReactivated() + 1);
                case SKIP -> counts.setSkipped(counts.getSkipped() + 1);
                case ERROR -> counts.setErrors(counts.getErrors() + 1);
            }
        }
        return counts;
    }
}
