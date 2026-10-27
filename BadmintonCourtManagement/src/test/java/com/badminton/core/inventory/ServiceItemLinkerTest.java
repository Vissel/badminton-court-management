package com.badminton.core.inventory;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.enums.InventoryItemType;
import com.badminton.repository.InventoryItemRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A catalog row saved from the Setup page has no {@code item_id}. The linker is
 * what makes such a row stock-tracked, so these tests pin the heal behaviour
 * that the service ledger depends on.
 */
@ExtendWith(MockitoExtension.class)
class ServiceItemLinkerTest {

    @Mock
    private InventoryItemRepository itemRepo;

    @Mock
    private ServiceRepository serviceRepo;

    @Mock
    private ShuttleBallRepository ballRepo;

    @InjectMocks
    private ServiceItemLinker linker;

    @Test
    void resolveLinksUnlinkedServiceToItemWithTheSameName() {
        Service service = new Service("sting", 15000);
        InventoryItem item = goodsItem(2, "sting");
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("sting", InventoryItemType.GOODS))
                .thenReturn(Optional.of(item));

        assertSame(item, linker.resolve(service));
        assertSame(item, service.getItem());
        verify(serviceRepo).save(service);
    }

    @Test
    void resolveKeepsAnExistingLinkWithoutQueryingTheItemCatalog() {
        Service service = new Service("sting", 15000);
        InventoryItem item = goodsItem(2, "sting");
        service.setItem(item);

        assertSame(item, linker.resolve(service));
        verifyNoInteractions(itemRepo, serviceRepo);
    }

    @Test
    void resolveLeavesPricingRowsUnlinked() {
        Service service = new Service("rentByTime", 100000);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("rentByTime", InventoryItemType.GOODS))
                .thenReturn(Optional.empty());

        assertNull(linker.resolve(service));
        assertNull(service.getItem());
        verify(serviceRepo, never()).save(service);
    }

    @Test
    void resolveLinksUnlinkedBallToShuttleItem() {
        ShuttleBall ball = new ShuttleBall("vinastar", 26000);
        InventoryItem item = goodsItem(1, "vinastar");
        item.setItemType(InventoryItemType.SHUTTLE_BALL);
        when(itemRepo.findByItemNameIgnoreCaseAndItemType("vinastar", InventoryItemType.SHUTTLE_BALL))
                .thenReturn(Optional.of(item));

        assertSame(item, linker.resolve(ball));
        assertSame(item, ball.getItem());
        verify(ballRepo).save(ball);
    }

    private InventoryItem goodsItem(int itemId, String itemName) {
        InventoryItem item = new InventoryItem(itemName, InventoryItemType.GOODS);
        ReflectionTestUtils.setField(item, "itemId", itemId);
        return item;
    }
}
