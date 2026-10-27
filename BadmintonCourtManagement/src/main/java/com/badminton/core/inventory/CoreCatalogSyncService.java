package com.badminton.core.inventory;

import com.badminton.constant.ApiConstant;
import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.enums.InventoryItemType;
import com.badminton.repository.InventoryItemRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.repository.StockMovementRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Keeps the sellable catalog ({@code service} / {@code shuttle_ball}, Setup
 * page)
 * and the stock catalog ({@code inventory_item}, Quản lý kho) in sync.
 *
 * <p>
 * Direction rules:
 * </p>
 * <ul>
 * <li>Setup → inventory: adding or restoring a catalog row ensures its
 * inventory
 * item exists, links it and mirrors the price into {@code retail_price};
 * deleting deactivates the item (the ledger is never touched).</li>
 * <li>Inventory → Setup: saving an item pushes name / Giá bán / active into the
 * catalog rows already linked to it.</li>
 * </ul>
 *
 * <p>
 * Each direction has a single entry point, so a save can never bounce back and
 * forth between the two pages.
 * </p>
 */
@Slf4j
@org.springframework.stereotype.Service
public class CoreCatalogSyncService {

    /**
     * Pricing rows that are not products and must never own an inventory item.
     */
    private static final Set<String> PROTECTED_NAMES = Set.of(
            ApiConstant.COST_IN_PERNSON,
            ApiConstant.RENT_BY_TIME,
            ApiConstant.ADVANCE_PAYMENT);

    private static final String BALL_BASE_UNIT = "quả";
    private static final String BALL_PACKAGE_UNIT = "ống";
    private static final int BALL_UNITS_PER_PACKAGE = 12;

    private final InventoryItemRepository itemRepo;
    private final ServiceRepository serviceRepo;
    private final ShuttleBallRepository ballRepo;
    private final StockMovementRepository movementRepo;

    public CoreCatalogSyncService(InventoryItemRepository itemRepo,
            ServiceRepository serviceRepo,
            ShuttleBallRepository ballRepo,
            StockMovementRepository movementRepo) {
        this.itemRepo = itemRepo;
        this.serviceRepo = serviceRepo;
        this.ballRepo = ballRepo;
        this.movementRepo = movementRepo;
    }

    public boolean isProtected(String name) {
        return name != null && PROTECTED_NAMES.contains(name.trim());
    }

    /**
     * Stock identity of a catalog product; created on first use so a service
     * added on the Setup page immediately shows up in Quản lý kho.
     */
    @Transactional
    public InventoryItem ensureItem(String name, InventoryItemType type,
            BigDecimal retailPrice, boolean active) {
        if (name == null || name.isBlank() || isProtected(name)) {
            return null;
        }
        String trimmed = name.trim();
        InventoryItem item = itemRepo.findByItemNameIgnoreCaseAndItemType(trimmed, type).orElse(null);
        // Idempotent on purpose: a GET must not update the whole item table on
        // every page view. Unchanged rows are returned without a save, so they
        // take no row lock at all.
        boolean created = false;
        boolean changed = false;
        if (item == null) {
            item = new InventoryItem(trimmed, type);
            applyUnitDefaults(item);
            created = true;
            changed = true;
        }
        if (retailPrice != null
                && (item.getRetailPrice() == null || item.getRetailPrice().compareTo(retailPrice) != 0)) {
            item.setRetailPrice(retailPrice);
            changed = true;
        }
        if (item.isActive() != active) {
            item.setActive(active);
            changed = true;
        }
        if (!changed) {
            return item;
        }
        InventoryItem saved = itemRepo.save(item);
        if (created) {
            log.info("Catalog sync: created inventory item '{}' ({})", trimmed, type);
        }
        return saved;
    }

    /**
     * Read-path heal used when the Setup page is opened: rows saved before the
     * inventory feature exist, and an unlinked row would show no remaining
     * quantity.
     *
     * <p>
     * This is the only write that happens while reading, so it is deliberately
     * conservative. It runs as a single ordered transaction (same sequence for
     * every request, so two concurrent page loads can never take the same item
     * rows in opposite order and deadlock) and it saves nothing when the rows
     * already match, which makes a steady-state page load read-only.
     * </p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureCatalogLinks(Collection<ShuttleBall> balls, Collection<Service> services) {
        if (balls != null) {
            balls.stream()
                    .filter(ShuttleBall::isActive)
                    .sorted(Comparator.comparing(ball -> sortKey(ball.getShuttleName())))
                    .forEach(this::healBall);
        }
        if (services != null) {
            services.stream()
                    .filter(Service::isActive)
                    .filter(service -> !isProtected(service.getSerName()))
                    .sorted(Comparator.comparing(service -> sortKey(service.getSerName())))
                    .forEach(this::healService);
        }
    }

    private void healBall(ShuttleBall ball) {
        BigDecimal price = mirrorPrice(ball.getCost(), ball.getItem());
        InventoryItem item = ensureItem(ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL, price, true);
        if (item != null) {
            link(ball, item, price);
        }
    }

    private void healService(Service service) {
        BigDecimal price = mirrorPrice(service.getCost(), service.getItem());
        InventoryItem item = ensureItem(service.getSerName(), InventoryItemType.GOODS, price, true);
        if (item != null) {
            link(service, item, price);
        }
    }

    /**
     * Giá bán of the linked item when it has one (Quản lý kho wins), else the
     * catalog price used to seed a new item.
     */
    private static BigDecimal mirrorPrice(float catalogCost, InventoryItem item) {
        if (item != null && item.getRetailPrice() != null) {
            return item.getRetailPrice();
        }
        return BigDecimal.valueOf(catalogCost);
    }

    private static String sortKey(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /**
     * Links a service row to its stock item and mirrors the billing price into
     * {@code retail_price} (and back), so both pages show the same number.
     */
    @Transactional
    public InventoryItem linkService(Service service, BigDecimal price) {
        if (service == null || isProtected(service.getSerName())) {
            return null;
        }
        InventoryItem item = ensureItem(service.getSerName(), InventoryItemType.GOODS, price, true);
        if (item == null) {
            return null;
        }
        return link(service, item, price);
    }

    /**
     * Same as {@link #linkService} for the shuttle-ball catalog.
     */
    @Transactional
    public InventoryItem linkShuttleBall(ShuttleBall ball, BigDecimal price) {
        if (ball == null) {
            return null;
        }
        InventoryItem item = ensureItem(ball.getShuttleName(), InventoryItemType.SHUTTLE_BALL, price, true);
        if (item == null) {
            return null;
        }
        return link(ball, item, price);
    }

    private InventoryItem link(Service service, InventoryItem item, BigDecimal price) {
        boolean changed = service.getSerId() == 0;
        if (price != null && Float.compare(service.getCost(), price.floatValue()) != 0) {
            service.setCost(price.floatValue());
            changed = true;
        }
        if (service.getItem() == null || service.getItem().getItemId() != item.getItemId()) {
            service.setItem(item);
            changed = true;
        }
        if (changed) {
            serviceRepo.save(service);
        }
        return item;
    }

    private InventoryItem link(ShuttleBall ball, InventoryItem item, BigDecimal price) {
        boolean changed = ball.getShuttleId() == 0;
        if (price != null && Float.compare(ball.getCost(), price.floatValue()) != 0) {
            ball.setCost(price.floatValue());
            changed = true;
        }
        if (ball.getItem() == null || ball.getItem().getItemId() != item.getItemId()) {
            ball.setItem(item);
            changed = true;
        }
        if (changed) {
            ballRepo.save(ball);
        }
        return item;
    }

    /**
     * Pushes an item saved in Quản lý kho into the catalog rows linked to it:
     * Giá bán → {@code cost}, rename → row name, inactive → row inactive.
     */
    @Transactional
    public void syncCatalogFromItem(InventoryItem item) {
        if (item == null || item.getItemName() == null) {
            return;
        }
        List<Service> services = serviceRepo.findAllByItemAndIsActive(item, true);
        for (Service service : services) {
            if (isProtected(service.getSerName())) {
                continue;
            }
            if (!item.isActive()) {
                service.setActive(false);
            } else {
                service.setSerName(item.getItemName());
                if (item.getRetailPrice() != null) {
                    service.setCost(item.getRetailPrice().floatValue());
                }
            }
            serviceRepo.save(service);
        }
        List<ShuttleBall> balls = ballRepo.findAllByItemAndIsActive(item, true);
        for (ShuttleBall ball : balls) {
            if (!item.isActive()) {
                ball.setActive(false);
            } else {
                ball.setShuttleName(item.getItemName());
                if (item.getRetailPrice() != null) {
                    ball.setCost(item.getRetailPrice().floatValue());
                }
            }
            ballRepo.save(ball);
        }
        if (!services.isEmpty() || !balls.isEmpty()) {
            log.info("Catalog sync: pushed item '{}' to {} service row(s) and {} shuttle row(s)",
                    item.getItemName(), services.size(), balls.size());
        }
    }

    /**
     * Setup delete: the product stops being sellable, stock history stays.
     */
    @Transactional
    public void deactivateCatalog(InventoryItem item) {
        if (item == null) {
            return;
        }
        if (item.isActive()) {
            item.setActive(false);
            itemRepo.save(item);
        }
        syncCatalogFromItem(item);
    }

    /**
     * Remaining base units, derived from the stock ledger.
     */
    @Transactional(readOnly = true)
    public long stockOnHand(InventoryItem item) {
        return item == null ? 0 : movementRepo.sumQuantityDeltaByItem(item);
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
}
