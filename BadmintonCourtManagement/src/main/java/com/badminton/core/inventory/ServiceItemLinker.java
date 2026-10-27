package com.badminton.core.inventory;

import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.enums.InventoryItemType;
import com.badminton.repository.InventoryItemRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Single place that keeps the sellable catalog ({@code service}) linked to the
 * inventory catalog ({@code inventory_item}).
 *
 * <p>
 * Rows created from the Setup page / product import are inserted without
 * {@code item_id}. An unlinked row silently disables stock tracking: the picker
 * gets no {@code itemId} (so the client never asks for the remaining count) and
 * adding the line to a player writes no RETAIL_SALE movement. Callers resolve
 * through this class instead of trusting the stored link.
 * </p>
 */
@Slf4j
@Component
public class ServiceItemLinker {

    private final InventoryItemRepository itemRepo;
    private final ServiceRepository serviceRepo;
    private final ShuttleBallRepository ballRepo;

    public ServiceItemLinker(InventoryItemRepository itemRepo, ServiceRepository serviceRepo,
            ShuttleBallRepository ballRepo) {
        this.itemRepo = itemRepo;
        this.serviceRepo = serviceRepo;
        this.ballRepo = ballRepo;
    }

    /**
     * Active GOODS item carrying the same name as the service, or null when the
     * service is a pure pricing row (costInPerson, rentByTime, advance payment).
     */
    public InventoryItem findByServiceName(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return null;
        }
        return itemRepo.findByItemNameIgnoreCaseAndItemType(serviceName.trim(), InventoryItemType.GOODS)
                .filter(InventoryItem::isActive)
                .orElse(null);
    }

    /**
     * Stock item the service draws from. A missing link is healed by name and
     * persisted, so the deduction and the badge keep working afterwards.
     *
     * @return the item, or null when the service is not stock-tracked
     */
    @Transactional
    public InventoryItem resolve(Service service) {
        if (service == null) {
            return null;
        }
        if (service.getItem() != null) {
            return service.getItem();
        }
        InventoryItem item = findByServiceName(service.getSerName());
        if (item == null) {
            return null;
        }
        service.setItem(item);
        serviceRepo.save(service);
        log.info("Linked service '{}' to inventory item '{}'", service.getSerName(), item.getItemName());
        return item;
    }

    /**
     * Active SHUTTLE_BALL item carrying the same name as the ball type.
     */
    public InventoryItem findByBallName(String ballName) {
        if (ballName == null || ballName.isBlank()) {
            return null;
        }
        return itemRepo.findByItemNameIgnoreCaseAndItemType(ballName.trim(), InventoryItemType.SHUTTLE_BALL)
                .filter(InventoryItem::isActive)
                .orElse(null);
    }

    /**
     * Stock item the ball type draws from; an unlinked Setup row is healed by
     * name so game consumption and the remaining count keep working.
     */
    @Transactional
    public InventoryItem resolve(ShuttleBall ball) {
        if (ball == null) {
            return null;
        }
        if (ball.getItem() != null) {
            return ball.getItem();
        }
        InventoryItem item = findByBallName(ball.getShuttleName());
        if (item == null) {
            return null;
        }
        ball.setItem(item);
        ballRepo.save(ball);
        log.info("Linked shuttle ball '{}' to inventory item '{}'", ball.getShuttleName(), item.getItemName());
        return item;
    }

    /**
     * Heals a batch saved from the Setup page; rows without an inventory match
     * stay unlinked on purpose.
     */
    @Transactional
    public void resolveAll(Collection<Service> services) {
        if (services == null) {
            return;
        }
        services.forEach(this::resolve);
    }

    /**
     * Same heal for the shuttle-ball catalog.
     */
    @Transactional
    public void resolveAllBalls(Collection<ShuttleBall> balls) {
        if (balls == null) {
            return;
        }
        balls.forEach(this::resolve);
    }
}
