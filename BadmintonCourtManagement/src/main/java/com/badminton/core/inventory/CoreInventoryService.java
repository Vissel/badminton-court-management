package com.badminton.core.inventory;

import com.badminton.entity.*;
import com.badminton.enums.InventoryItemType;
import com.badminton.enums.MovementReferenceType;
import com.badminton.enums.StockMovementType;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.game.ShuttleBallModel;
import com.badminton.model.inventory.ConsumedBallsInGame;
import com.badminton.model.inventory.ConsumedItemsModel;
import com.badminton.repository.InventoryItemRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.repository.StockMovementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CoreInventoryService {
    private static final int LOW_STOCK_THRESHOLD = 10;
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
    @Autowired
    private InventoryItemRepository inventoryItemRepository;
    @Autowired
    private StockMovementRepository stockMovementRepository;
    @Autowired
    private ShuttleBallRepository shuttleRepo;
    @Autowired
    private ServiceRepository serviceRepo;

    /**
     * Deducts the shuttle balls selected on the game from stock by writing
     * GAME_CONSUMPTION movements for every ball in the game's shuttle map.
     */
    public Boolean consumeShuttleBallsInGame(ConsumedBallsInGame consumedBallsInGame) {
        List<ConsumedItemsModel> consumedItems = convertBallsToItems(consumedBallsInGame);
        return recordItemConsumption(consumedItems);

    }

    public Boolean consumeShuttleBallsForGame(Game game) {
        if (game == null || game.getShuttleMap() == null) {
            return Boolean.FALSE;
        }
        ConsumedBallsInGame consumedBallsInGame = new ConsumedBallsInGame();
        consumedBallsInGame.setInGameId(game.getGameId());
        List<ShuttleBallModel> ballModels = new ArrayList<>();
        for (GameShuttleMap mapping : game.getShuttleMap()) {
            if (mapping == null || mapping.getShuttleBall() == null) {
                continue;
            }
            ShuttleBall ball = mapping.getShuttleBall();
            ShuttleBallModel model = new ShuttleBallModel();
            model.setName(ball.getShuttleName());
            model.setCost(BigDecimal.valueOf(ball.getCost()));
            model.setQuantity(mapping.getShuttleNumber());
            ballModels.add(model);
        }
        consumedBallsInGame.setShuttleBallModelList(ballModels);
        return consumeShuttleBallsInGame(consumedBallsInGame);
    }

    private List<ConsumedItemsModel> convertBallsToItems(ConsumedBallsInGame consumedBallsInGame) {
        List<ConsumedItemsModel> result = new ArrayList<>();

        Long gameId = (long) consumedBallsInGame.getInGameId();
        for (ShuttleBallModel ball : consumedBallsInGame.getShuttleBallModelList()) {
            ConsumedItemsModel item = new ConsumedItemsModel();
            item.setItemName(ball.getName());
            item.setItemType(InventoryItemType.SHUTTLE_BALL);
            item.setConsumeType(StockMovementType.GAME_CONSUMPTION);
            item.setQuantity(ball.getQuantity());
            item.setSign(-1);
            item.setReferenceType(MovementReferenceType.GAME);
            item.setReferenceId(gameId);
            item.setNote("Game " + gameId);
            result.add(item);
        }
        return result;
    }

    /**
     * Records consumed items (e.g. shuttle balls taken out for a game) by
     * writing negative GAME_CONSUMPTION movements. Stock on hand is re-read
     * right after each deduction and a warning is logged for the admin when
     * it drops to the low-stock threshold.
     */
    @Transactional
    public Boolean recordItemConsumption(List<ConsumedItemsModel> consumedItems) {
        writeItemMovements(consumedItems);
        return Boolean.TRUE;
    }

    public Boolean recordItemConsumption(Map<InventoryItem, Integer> items, Long gameId) {
        if (items == null) {
            return Boolean.TRUE;
        }
        List<ConsumedItemsModel> consumedItems = new ArrayList<>();
        for (Map.Entry<InventoryItem, Integer> entry : items.entrySet()) {
            InventoryItem item = entry.getKey();
            Integer qty = entry.getValue();
            if (item == null || qty == null || qty <= 0) {
                continue;
            }
            ConsumedItemsModel model = new ConsumedItemsModel();
            model.setItemName(item.getItemName());
            model.setItemType(item.getItemType());
            model.setConsumeType(StockMovementType.GAME_CONSUMPTION);
            model.setQuantity(qty);
            model.setSign(-1);
            model.setReferenceType(MovementReferenceType.GAME);
            model.setReferenceId(gameId);
            model.setNote(gameId != null ? "Game " + gameId : "Item consumption");
            consumedItems.add(model);
        }
        writeItemMovements(consumedItems);
        return Boolean.TRUE;
    }

    /**
     * Revokes a previous consumption: the items go back to stock via positive
     * RETURN movements (e.g. a terminated game returns its shuttle balls).
     */
    @Transactional
    public Boolean revokeItemConsumption(Map<InventoryItem, Integer> items, Long gameId) {
        if (items == null) {
            return Boolean.TRUE;
        }
        List<ConsumedItemsModel> consumedItems = new ArrayList<>();
        for (Map.Entry<InventoryItem, Integer> entry : items.entrySet()) {
            InventoryItem item = entry.getKey();
            Integer qty = entry.getValue();
            if (item == null || qty == null || qty <= 0) {
                continue;
            }
            ConsumedItemsModel model = new ConsumedItemsModel();
            model.setItemName(item.getItemName());
            model.setItemType(item.getItemType());
            model.setConsumeType(StockMovementType.RETURN);
            model.setQuantity(qty);
            model.setSign(1);
            model.setReferenceType(MovementReferenceType.GAME);
            model.setReferenceId(gameId);
            model.setNote(gameId != null ? "Return game " + gameId : "Item return");
            consumedItems.add(model);
        }
        writeItemMovements(consumedItems);
        return Boolean.TRUE;
    }

    /**
     * Confirms service lines are actually paid: writes negative RETAIL_SALE
     * movements for every line that resolves to a stockable item.
     */
    @Transactional
    public Boolean retailItem(List<ServiceDTO> services, Long refId) {
        if (services == null) {
            return Boolean.TRUE;
        }
        for (ServiceDTO line : services) {
            int qty = line.getQuantity() != null && line.getQuantity() > 0 ? line.getQuantity() : 1;
            InventoryItem item = resolveServiceItem(line);
            if (item == null) {
                continue;
            }
            StockMovement movement = baseMovement(item, StockMovementType.RETAIL_SALE,
                    -qty, line.getServiceName());
            movement.setRefType(MovementReferenceType.PAYMENT);
            movement.setRefId(refId);
            stockMovementRepository.save(movement);
            warnIfLowStock(item);
        }
        return Boolean.TRUE;
    }

    @Transactional
    public void writeItemMovements(List<ConsumedItemsModel> consumedItemsModels) {
        List<StockMovement> stockMovementList = new ArrayList<>();
        for (ConsumedItemsModel consumedItem : consumedItemsModels) {
            if (consumedItem == null || consumedItem.getItemName() == null || consumedItem.getItemName().isBlank()) {
                continue;
            }
            InventoryItemType itemType = consumedItem.getItemType() != null
                    ? consumedItem.getItemType() : InventoryItemType.SHUTTLE_BALL;
            InventoryItem item = inventoryItemRepository
                    .findByItemNameIgnoreCaseAndItemType(consumedItem.getItemName(), itemType)
                    .orElse(null);
            if (item == null) {
                log.warn("Cannot write movement: no inventory item for '{}' of type {}",
                        consumedItem.getItemName(), itemType);
                continue;
            }
            int qty = consumedItem.getQuantity();
            if (qty <= 0) {
                continue;
            }
            StockMovementType movementType = consumedItem.getConsumeType() != null
                    ? consumedItem.getConsumeType() : StockMovementType.GAME_CONSUMPTION;
            int sign = consumedItem.getSign() != 0 ? consumedItem.getSign() : -1;
            String note = consumedItem.getNote() != null && !consumedItem.getNote().isBlank()
                    ? consumedItem.getNote()
                    : (consumedItem.getReferenceId() != null ? "Game " + consumedItem.getReferenceId() : "Item consumption");
            StockMovement movement = baseMovement(item, movementType, sign * qty, note);
            MovementReferenceType refType = consumedItem.getReferenceType() != null
                    ? consumedItem.getReferenceType() : MovementReferenceType.GAME;
            movement.setRefType(refType);
            movement.setRefId(consumedItem.getReferenceId());
            stockMovementList.add(movement);
        }
        stockMovementRepository.saveAll(stockMovementList);
    }

    /**
     * Stock is ledger-derived, so on-hand is always real time. After a write,
     * re-sum the ledger and warn the admin once the item is low.
     */
    private void warnIfLowStock(InventoryItem item) {
        long stock = stockMovementRepository.sumQuantityDeltaByItem(item);
        if (stock <= LOW_STOCK_THRESHOLD) {
            log.warn("Low stock for '{}': {} {} left", item.getItemName(), stock,
                    item.getUnit() != null ? item.getUnit() : "");
        }
    }

    private InventoryItem resolveServiceItem(ServiceDTO line) {
        if (line.getItemId() != null) {
            InventoryItem item = inventoryItemRepository.findById(line.getItemId()).orElse(null);
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

    /**
     * Resolves the stock item for a catalog ball. Catalog rows created after
     * the initial backfill (AdminServiceImpl, product import) can carry
     * {@code item_id = NULL} - heal the link lazily so stock always deducts.
     */
    private InventoryItem resolveItemForBall(ShuttleBall ball) {
        if (ball.getItem() != null) {
            return ball.getItem();
        }
        InventoryItem item = inventoryItemRepository
                .findByItemNameIgnoreCaseAndItemType(ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL)
                .orElseGet(() -> {
                    InventoryItem created = new InventoryItem(ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL);
                    applyUnitDefaults(created);
                    return inventoryItemRepository.save(created);
                });
        ball.setItem(item);
        shuttleRepo.save(ball);
        return item;
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

    private StockMovement baseMovement(InventoryItem item, StockMovementType type,
                                       int delta, String note) {
        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setMovementType(type);
        movement.setQuantityDelta(delta);
        movement.setNote(note);
        return movement;
    }
}
