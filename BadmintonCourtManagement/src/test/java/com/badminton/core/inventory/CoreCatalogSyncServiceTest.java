package com.badminton.core.inventory;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.enums.InventoryItemType;
import com.badminton.repository.InventoryItemRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The Setup page and Quản lý kho must never drift apart: a catalog row owns
 * nothing but the price, everything else (stock, Giá bán, active) lives on the
 * inventory item. These tests pin both sync directions.
 */
@ExtendWith(MockitoExtension.class)
class CoreCatalogSyncServiceTest {

    @Mock
    private InventoryItemRepository itemRepo;

    @Mock
    private ServiceRepository serviceRepo;

    @Mock
    private ShuttleBallRepository ballRepo;

    @Mock
    private StockMovementRepository movementRepo;

    private CoreCatalogSyncService sync;

    @BeforeEach
    void setUp() {
        sync = new CoreCatalogSyncService(itemRepo, serviceRepo, ballRepo, movementRepo);
        lenient().when(itemRepo.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void linkServiceCreatesItemWithMirroredPriceAndLinksTheRow() {
        Service service = new Service("sting", 15000);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("sting", InventoryItemType.GOODS))
                .thenReturn(Optional.empty());

        InventoryItem item = sync.linkService(service, BigDecimal.valueOf(15000));

        assertEquals("sting", item.getItemName());
        assertEquals(InventoryItemType.GOODS, item.getItemType());
        assertEquals(BigDecimal.valueOf(15000), item.getRetailPrice());
        assertTrue(item.isActive());
        assertSame(item, service.getItem());
        verify(serviceRepo).save(service);
    }

    @Test
    void linkServiceReusesTheInactiveItemInsteadOfDuplicatingIt() {
        Service service = new Service("sting", 16000);
        InventoryItem existing = item(2, "sting", InventoryItemType.GOODS, 15000, false);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("sting", InventoryItemType.GOODS))
                .thenReturn(Optional.of(existing));

        InventoryItem item = sync.linkService(service, BigDecimal.valueOf(16000));

        assertSame(existing, item);
        assertTrue(item.isActive());
        assertEquals(BigDecimal.valueOf(16000), item.getRetailPrice());
        assertEquals(16000f, service.getCost());
        assertSame(existing, service.getItem());
    }

    @Test
    void pricingRowsNeverGetAStockItem() {
        Service rentByTime = new Service("rentByTime", 100000);

        assertNull(sync.linkService(rentByTime, BigDecimal.valueOf(100000)));
        assertNull(rentByTime.getItem());
        verifyNoInteractions(itemRepo);
        verify(serviceRepo, never()).save(any());
    }

    @Test
    void linkShuttleBallAppliesBallUnitDefaults() {
        ShuttleBall ball = new ShuttleBall("vinastar", 26000);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("vinastar", InventoryItemType.SHUTTLE_BALL))
                .thenReturn(Optional.empty());

        InventoryItem item = sync.linkShuttleBall(ball, BigDecimal.valueOf(26000));

        assertEquals("quả", item.getUnit());
        assertEquals("ống", item.getPackageUnit());
        assertEquals(12, item.getUnitsPerPackage());
        assertSame(item, ball.getItem());
        verify(ballRepo).save(ball);
    }

    @Test
    void syncCatalogFromItemPushesPriceAndNameIntoActiveRows() {
        InventoryItem item = item(5, "sting new", InventoryItemType.GOODS, 18000, true);
        Service service = new Service("sting old", 15000);
        service.setItem(item);
        when(serviceRepo.findAllByItemAndIsActive(item, true)).thenReturn(List.of(service));

        sync.syncCatalogFromItem(item);

        assertEquals("sting new", service.getSerName());
        assertEquals(18000f, service.getCost());
        assertTrue(service.isActive());
        verify(serviceRepo).save(service);
    }

    @Test
    void syncCatalogFromItemDeactivatesRowsOfAnInactiveItem() {
        InventoryItem item = item(5, "sting", InventoryItemType.GOODS, 15000, false);
        ShuttleBall ball = new ShuttleBall("vinastar", 26000);
        when(ballRepo.findAllByItemAndIsActive(item, true)).thenReturn(List.of(ball));

        sync.syncCatalogFromItem(item);

        assertFalse(ball.isActive());
        verify(ballRepo).save(ball);
    }

    @Test
    void deactivateCatalogStopsSellingWithoutTouchingTheLedger() {
        InventoryItem item = item(5, "sting", InventoryItemType.GOODS, 15000, true);
        Service service = new Service("sting", 15000);
        when(serviceRepo.findAllByItemAndIsActive(item, true)).thenReturn(List.of(service));

        sync.deactivateCatalog(item);

        assertFalse(item.isActive());
        assertFalse(service.isActive());
        verify(itemRepo).save(item);
        verify(serviceRepo).save(service);
        verifyNoInteractions(movementRepo);
    }

    @Test
    void ensureCatalogLinksIsReadOnlyWhenEveryRowAlreadyMatches() {
        InventoryItem item = item(2, "sting", InventoryItemType.GOODS, 15000, true);
        Service service = persistedService(5, "sting", 15000, item);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("sting", InventoryItemType.GOODS))
                .thenReturn(Optional.of(item));

        sync.ensureCatalogLinks(List.of(), List.of(service));

        // opening the Setup page must never rewrite a row that is already in sync
        verify(itemRepo, never()).save(any());
        verify(serviceRepo, never()).save(any());
    }

    @Test
    void ensureCatalogLinksCreatesTheMissingItemForAnUnlinkedRow() {
        Service service = persistedService(5, "tra duong", 12000, null);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("tra duong", InventoryItemType.GOODS))
                .thenReturn(Optional.empty());

        sync.ensureCatalogLinks(List.of(), List.of(service));

        verify(itemRepo).save(any(InventoryItem.class));
        verify(serviceRepo).save(service);
        assertNotNull(service.getItem());
        assertEquals(12000, service.getItem().getRetailPrice().intValue());
    }

    private Service persistedService(int serId, String name, float cost, InventoryItem item) {
        Service service = new Service(name, cost);
        ReflectionTestUtils.setField(service, "serId", serId);
        service.setItem(item);
        return service;
    }

    private InventoryItem item(int itemId, String name, InventoryItemType type,
            Integer price, boolean active) {
        InventoryItem item = new InventoryItem(name, type);
        ReflectionTestUtils.setField(item, "itemId", itemId);
        item.setRetailPrice(price != null ? BigDecimal.valueOf(price) : null);
        item.setActive(active);
        return item;
    }
}
