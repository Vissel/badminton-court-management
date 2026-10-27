package com.badminton.service;

import com.badminton.BadmintonCourtManagementApplication;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.InventoryItem;
import com.badminton.enums.InventoryItemType;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.requestmodel.ServiceRequest;
import com.badminton.core.inventory.ServiceItemLinker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The add-service endpoint is the only place a carried good leaves the shelf
 * before payment, so it must hand the ledger a line that already knows its
 * stock
 * item.
 */
@ExtendWith(MockitoExtension.class)
class CourtServicesServiceRetailSaleTest {

    @Mock
    private BadmintonCourtManagementApplication application;
    @Mock
    private ServiceRepository serviceRepo;
    @Mock
    private SessionServiceImpl session;
    @Mock
    private AvailablePlayerRepository avaPlayerRepo;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private ServiceItemLinker serviceItemLinker;

    private CourtServicesService courtServicesService;

    @BeforeEach
    void setUp() {
        // the collaborators are @Autowired fields, so wire them by hand
        courtServicesService = new CourtServicesService(application);
        ReflectionTestUtils.setField(courtServicesService, "serviceRepo", serviceRepo);
        ReflectionTestUtils.setField(courtServicesService, "session", session);
        ReflectionTestUtils.setField(courtServicesService, "avaPlayerRepo", avaPlayerRepo);
        ReflectionTestUtils.setField(courtServicesService, "inventoryService", inventoryService);
        ReflectionTestUtils.setField(courtServicesService, "serviceItemLinker", serviceItemLinker);
    }

    @Test
    void addServiceToPlayerRecordsRetailSaleForTheLinkedItem() {
        AvailablePlayer player = new AvailablePlayer();
        player.setAvaId(7L);
        player.setServices("[]");
        when(session.getAvailablePlayerInActiveSession("An")).thenReturn(player);

        com.badminton.entity.Service catalogRow = new com.badminton.entity.Service("sting", 15000);
        InventoryItem item = new InventoryItem("sting", InventoryItemType.GOODS);
        ReflectionTestUtils.setField(item, "itemId", 2);
        when(serviceRepo.findBySerName("sting")).thenReturn(Optional.of(catalogRow));
        when(serviceItemLinker.resolve(catalogRow)).thenReturn(item);

        assertTrue(courtServicesService.addServiceToAvailablePlayer(new ServiceRequest("sting", "15000"), "An"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ServiceDTO>> ledgerLines = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).recordRetailSales(ledgerLines.capture(), eq(7L));
        ServiceDTO recorded = ledgerLines.getValue().getFirst();
        assertEquals("sting", recorded.getServiceName());
        assertEquals(Integer.valueOf(2), recorded.getItemId());
        assertEquals(Integer.valueOf(1), recorded.getQuantity());

        // the stored line keeps the link, so later add/remove deltas hit the same key
        String stored = player.getCurrentServices();
        assertNotNull(stored);
        assertTrue(stored.contains("\"itemId\":2"), stored);
    }

    @Test
    void addServiceToPlayerLeavesPricingRowsWithoutAnItem() {
        AvailablePlayer player = new AvailablePlayer();
        player.setAvaId(9L);
        player.setServices("[]");
        when(session.getAvailablePlayerInActiveSession("An")).thenReturn(player);

        com.badminton.entity.Service catalogRow = new com.badminton.entity.Service("rentByTime", 100000);
        when(serviceRepo.findBySerName("rentByTime")).thenReturn(Optional.of(catalogRow));
        when(serviceItemLinker.resolve(catalogRow)).thenReturn(null);

        assertTrue(courtServicesService.addServiceToAvailablePlayer(
                new ServiceRequest("rentByTime", "100000"), "An"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ServiceDTO>> ledgerLines = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).recordRetailSales(ledgerLines.capture(), eq(9L));
        assertEquals(null, ledgerLines.getValue().getFirst().getItemId());
    }
}
