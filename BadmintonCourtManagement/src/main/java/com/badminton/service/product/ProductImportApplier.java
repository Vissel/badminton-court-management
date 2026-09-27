package com.badminton.service.product;

import com.badminton.constant.ApiConstant;
import com.badminton.entity.ShuttleBall;
import com.badminton.enums.ImportAction;
import com.badminton.enums.ProductImportMode;
import com.badminton.enums.ProductSheet;
import com.badminton.model.product.ProductImportPlan;
import com.badminton.model.product.ProductImportRow;
import com.badminton.repository.ServiceRepositoty;
import com.badminton.repository.ShuttleBallRepositoty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Applies a cached product import plan in a single transaction.
 * Kept as a separate bean so {@link Transactional} proxies correctly and any
 * failure rolls back the whole import (no partial catalog state).
 */
@Slf4j
@Component
public class ProductImportApplier {

    @Autowired
    private ShuttleBallRepositoty shuttleRepo;

    @Autowired
    private ServiceRepositoty serviceRepo;

    /**
     * Pricing settings rows that must never be imported, exported or deleted.
     */
    public static final Set<String> PROTECTED_SERVICE_NAMES = Set.of(
            ApiConstant.COST_IN_PERNSON,
            ApiConstant.RENT_BY_TIME);

    @Transactional
    public void apply(ProductImportPlan plan) {
        for (ProductImportRow row : plan.getRows()) {
            applyRow(row);
        }
        if (plan.getMode() == ProductImportMode.REPLACE) {
            deactivateMissingShuttleBalls(plan.getImportedShuttleNames());
            deactivateMissingServices(plan.getImportedServiceNames());
        }
    }

    private void applyRow(ProductImportRow row) {
        if (row.getAction() == null) {
            return;
        }
        switch (row.getAction()) {
            case ADD -> saveNew(row);
            case UPDATE -> applyUpdate(row);
            case REACTIVATE -> applyReactivate(row);
            default -> {
                // SKIP / ERROR - nothing to persist
            }
        }
    }

    private void applyUpdate(ProductImportRow row) {
        // UPDATE = deactivate the old row and insert a new one so historical
        // references (GameShuttleMap, AvailablePlayer.services) keep pointing
        // at the price that was actually used.
        if (!deactivateRef(row)) {
            // Row was removed between preview and commit - fall back to ADD.
            saveNew(row);
            return;
        }
        saveNew(row);
    }

    private void applyReactivate(ProductImportRow row) {
        Object ref = row.getEntityRef();
        if (row.getSheet() == ProductSheet.SHUTTLE_BALL && ref instanceof ShuttleBall ball) {
            shuttleRepo.findById(ball.getShuttleId()).ifPresentOrElse(entity -> {
                entity.setActive(true);
                entity.setCost(row.getCost());
                shuttleRepo.save(entity);
            }, () -> saveNew(row));
        } else if (row.getSheet() == ProductSheet.SERVICE && ref instanceof com.badminton.entity.Service service) {
            serviceRepo.findById(service.getSerId()).ifPresentOrElse(entity -> {
                entity.setActive(true);
                entity.setCost(row.getCost());
                serviceRepo.save(entity);
            }, () -> saveNew(row));
        } else {
            saveNew(row);
        }
    }

    private boolean deactivateRef(ProductImportRow row) {
        Object ref = row.getEntityRef();
        if (row.getSheet() == ProductSheet.SHUTTLE_BALL && ref instanceof ShuttleBall ball) {
            return shuttleRepo.findById(ball.getShuttleId())
                    .map(entity -> {
                        entity.setActive(false);
                        shuttleRepo.save(entity);
                        return true;
                    })
                    .orElse(false);
        }
        if (row.getSheet() == ProductSheet.SERVICE && ref instanceof com.badminton.entity.Service service) {
            return serviceRepo.findById(service.getSerId())
                    .map(entity -> {
                        entity.setActive(false);
                        serviceRepo.save(entity);
                        return true;
                    })
                    .orElse(false);
        }
        return false;
    }

    private void saveNew(ProductImportRow row) {
        if (row.getSheet() == ProductSheet.SHUTTLE_BALL) {
            shuttleRepo.save(new ShuttleBall(row.getName(), row.getCost()));
        } else {
            serviceRepo.save(new com.badminton.entity.Service(row.getName(), row.getCost()));
        }
    }

    private void deactivateMissingShuttleBalls(Set<String> importedNames) {
        List<ShuttleBall> actives = shuttleRepo.findAllByIsActive(true);
        for (ShuttleBall ball : actives) {
            if (!importedNames.contains(ProductExcelParser.normalize(ball.getShuttleName()))) {
                ball.setActive(false);
                shuttleRepo.save(ball);
            }
        }
    }

    private void deactivateMissingServices(Set<String> importedNames) {
        List<com.badminton.entity.Service> actives = serviceRepo.findAllByIsActive(true);
        for (com.badminton.entity.Service service : actives) {
            if (PROTECTED_SERVICE_NAMES.contains(service.getSerName())) {
                continue;
            }
            if (!importedNames.contains(ProductExcelParser.normalize(service.getSerName()))) {
                service.setActive(false);
                serviceRepo.save(service);
            }
        }
    }
}
