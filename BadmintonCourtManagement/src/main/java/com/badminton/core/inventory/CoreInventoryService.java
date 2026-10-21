package com.badminton.core.inventory;

import com.badminton.core.game.CoreGameService;
import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.InventoryItem;
import com.badminton.entity.StockMovement;
import com.badminton.enums.InventoryItemType;
import com.badminton.enums.MovementReferenceType;
import com.badminton.enums.StockMovementType;
import com.badminton.exception.BusinessException;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.inventory.ConsumedItemsModel;
import com.badminton.model.inventory.GameEndConfirmDTO;
import com.badminton.repository.InventoryItemRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.repository.StockMovementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
    @Autowired
    private CoreGameService coreGameService;

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

    @Transactional
    public boolean confirmMovementItemEndGame(GameEndConfirmDTO gameEndConfirmDTO) {
        try {
            Game game = coreGameService.findActiveGame(gameEndConfirmDTO.getCourtId());
            List<StockMovement> movements = stockMovementRepository.findByRefIdAndRefType((long) game.getGameId(), MovementReferenceType.GAME);
            if (gameEndConfirmDTO.isFinish()) {
                // Finish: validate that game shuttleMap matches stock movements
                validateGameShuttleConsumption(game, movements);
                return true;
            } else {
                // Termination: calculate net consumption and return only what was actually consumed
                Map<String, Integer> netConsumption = new HashMap<>();

                for (StockMovement movement : movements) {
                    String itemKey = movement.getItem().getItemName() + "|" + movement.getItem().getItemType().name();
                    int delta = movement.getQuantityDelta();

                    if (movement.getMovementType() == StockMovementType.GAME_CONSUMPTION) {
                        netConsumption.put(itemKey, netConsumption.getOrDefault(itemKey, 0) + delta);
                    } else if (movement.getMovementType() == StockMovementType.RETURN) {
                        netConsumption.put(itemKey, netConsumption.getOrDefault(itemKey, 0) + delta);
                    }
                }

                List<ConsumedItemsModel> returnItems = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : netConsumption.entrySet()) {
                    int netDelta = entry.getValue();
                    if (netDelta < 0) {
                        // Net consumption exists - return it
                        String[] parts = entry.getKey().split("\\|");
                        ConsumedItemsModel model = new ConsumedItemsModel();
                        model.setItemName(parts[0]);
                        model.setItemType(InventoryItemType.valueOf(parts[1]));
                        model.setConsumeType(StockMovementType.RETURN);
                        model.setQuantity(Math.abs(netDelta));
                        model.setSign(1);
                        model.setReferenceType(MovementReferenceType.GAME);
                        model.setReferenceId((long) game.getGameId());
                        model.setNote("Terminated game " + game.getGameId() + " - return items");
                        returnItems.add(model);
                    }
                }
                if (!returnItems.isEmpty()) {
                    writeItemMovements(returnItems);
                }
            }
        } catch (BusinessException e) {
            log.warn("Game in courtId [{}] not found for shuttle consumption validation", gameEndConfirmDTO.getCourtId());
        }
        return true;
    }

    /**
     * Validates that the shuttle balls tracked in Game.shuttleMap match the
     * GAME_CONSUMPTION stock movements for the game. Logs warnings for discrepancies.
     */
    private void validateGameShuttleConsumption(Game game, List<StockMovement> movements) {
        List<GameShuttleMap> shuttleMap = game.getShuttleMap();
        if (shuttleMap == null || shuttleMap.isEmpty()) {
            // No shuttle balls in game - check if movements exist
            if (!movements.isEmpty()) {
                log.warn("Game in courtId [{}] has no shuttleMap but has [{}] stock movements - data inconsistency", game.getCourt().getCourtId(), movements.size());
            }
            return;
        }

        // Build expected consumption from shuttleMap
        Map<Integer, Integer> expectedConsumption = new HashMap<>();
        for (GameShuttleMap map : shuttleMap) {
            InventoryItem item = map.getShuttleBall().getItem();
            if (item != null) {
                expectedConsumption.put(item.getItemId(), expectedConsumption.getOrDefault(item.getItemId(), 0) + map.getShuttleNumber());
            } else {
                log.warn("ShuttleBall {} has no linked InventoryItem - cannot validate stock movement", map.getShuttleBall().getShuttleName());
            }
        }

        // Sum actual GAME_CONSUMPTION movements (ignoring RETURN - those are for termination)
        Map<Integer, Integer> actualConsumption = new HashMap<>();
        for (StockMovement movement : movements) {
            if (movement.getMovementType() == StockMovementType.GAME_CONSUMPTION) {
                int itemId = movement.getItem().getItemId();
                // GAME_CONSUMPTION has negative delta, sum absolute values
                actualConsumption.put(itemId, actualConsumption.getOrDefault(itemId, 0) + Math.abs(movement.getQuantityDelta()));
            }
        }

        // Compare - both are positive quantities
        for (Map.Entry<Integer, Integer> entry : expectedConsumption.entrySet()) {
            int itemId = entry.getKey();
            int expected = entry.getValue();
            int actual = actualConsumption.getOrDefault(itemId, 0);
            if (expected != actual) {
                InventoryItem item = inventoryItemRepository.findById(itemId).orElse(null);
                String itemName = item != null ? item.getItemName() : "unknown";
                log.warn("Game in courtId [{}] shuttle consumption mismatch for item '[{}]': expected [{}], actual [{}]",
                        game.getCourt().getCourtId(), itemName, expected, actual);
            }
        }

        // Check for extra GAME_CONSUMPTION movements not in shuttleMap
        for (Map.Entry<Integer, Integer> entry : actualConsumption.entrySet()) {
            if (!expectedConsumption.containsKey(entry.getKey())) {
                InventoryItem item = inventoryItemRepository.findById(entry.getKey()).orElse(null);
                String itemName = item != null ? item.getItemName() : "unknown";
                log.warn("Game in courtId [{}] has GAME_CONSUMPTION for item '{}' (qty {}) but not in shuttleMap",
                        game.getCourt().getCourtId(), itemName, entry.getValue());
            }
        }
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
