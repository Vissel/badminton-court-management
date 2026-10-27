package com.badminton.service;

import com.badminton.config.SecurityConfig;
import com.badminton.constant.ApiConstant;
import com.badminton.core.inventory.CoreCatalogSyncService;
import com.badminton.core.inventory.ServiceItemLinker;
import com.badminton.entity.Court;
import com.badminton.entity.InventoryItem;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.exception.GlobalExceptionHandler;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.CourtRepositoty;
import com.badminton.repository.ServiceRepository;
import com.badminton.repository.ShuttleBallRepository;
import com.badminton.requestmodel.SetUpServiceDTO;
import com.badminton.requestmodel.SetUpServiceRequest;
import com.badminton.response.ServiceResponse;
import com.badminton.response.SetUpServiceResponse;
import com.badminton.response.result.ShuttleBallResponse;
import com.badminton.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    /** Attempts (including the first one) for a write aborted by a DB deadlock. */
    private static final int LOCK_CONTENTION_RETRIES = 3;
    private static final int RETRY_BACKOFF_MS = 80;
    private static final int MYSQL_DEADLOCK = 1213;
    private static final int MYSQL_LOCK_WAIT_TIMEOUT = 1205;

    private final SecurityConfig securityConfig;

    private final UrlBasedCorsConfigurationSource corsConfigurationSource;

    private final GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private CourtRepositoty courtRepo;
    @Autowired
    private ShuttleBallRepository shuttleRepo;
    @Autowired
    private ServiceRepository serviceRepo;
    @Autowired
    private ServiceItemLinker serviceItemLinker;
    @Autowired
    private CoreCatalogSyncService catalogSync;

    private static final String COURT_STR = "Sân ";

    AdminServiceImpl(GlobalExceptionHandler globalExceptionHandler,
            UrlBasedCorsConfigurationSource corsConfigurationSource, SecurityConfig securityConfig) {
        this.globalExceptionHandler = globalExceptionHandler;
        this.corsConfigurationSource = corsConfigurationSource;
        this.securityConfig = securityConfig;
    }

    @Override
    public boolean setUpService(SetUpServiceDTO setupServiceDTO) {
        try {
            checkAndCreateCourt(setupServiceDTO.getTotalCourt());
            // save shuttle infor
            if (!setupServiceDTO.getShuttleBalls().isEmpty()) {
                ShuttleBallDTO ballDTO = setupServiceDTO.getShuttleBalls().get(0);
                if (!ballDTO.getShuttleName().isBlank()) {
                    ShuttleBall savedBall = findAddedShuttleBall(ballDTO.getShuttleName(), ballDTO.getShuttleCost());
                    if (savedBall != null) {
                        syncBall(savedBall);
                    }
                }
            }
            if (!setupServiceDTO.getServices().isEmpty()) {
                // saved services
                List<Service> listServices = setupServiceDTO.getServices().stream()
                        .map(serDTO -> findAddedService(serDTO.getServiceName(), serDTO.getCost()))
                        .filter(Objects::nonNull).collect(Collectors.toList());
                serviceRepo.saveAll(listServices);
                // Setup only writes the price; without the item link the catalog row
                // never deducts stock and never shows the remaining count.
                syncServices(listServices);
            }
            if (setupServiceDTO.getCostInPerson() != MoneyUtils.DEFAULT) {
                savePricingRow(ApiConstant.COST_IN_PERNSON, setupServiceDTO.getCostInPerson());
            }
            return true;
        } catch (Exception e) {
            log.error("Failed to add setup services", e);
            return false;
        }
    }

    @Override
    public boolean updateSetUpService(SetUpServiceRequest setupServiceRequest) {
        try {

            checkAndCreateCourt(setupServiceRequest.getTotalCourt());

            // save shuttle infor
            updateShuttleBalls(setupServiceRequest);
            updateServices(setupServiceRequest);

            if (setupServiceRequest.getCostInPerson() != MoneyUtils.DEFAULT) {
                savePricingRow(ApiConstant.COST_IN_PERNSON, setupServiceRequest.getCostInPerson());
            }

            if (setupServiceRequest.getRentByTime() != MoneyUtils.DEFAULT) {
                savePricingRow(ApiConstant.RENT_BY_TIME, setupServiceRequest.getRentByTime());
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to update setup services", e);
            return false;
        }
    }

    /**
     * Pricing rows (costInPerson, rentByTime) are plain settings, never products.
     */
    private void savePricingRow(String name, float cost) {
        Optional<Service> optSer = serviceRepo.findBySerName(name);
        Service savedService;
        if (optSer.isPresent()) {
            savedService = optSer.get();
            savedService.setCost(cost);
        } else {
            savedService = new Service(name, cost);
        }
        serviceRepo.save(savedService);
    }

    private void updateServices(SetUpServiceRequest setupServiceRequest) {
        List<Service> actives = serviceRepo.findAllByIsActive(true);
        List<Service> deActives = serviceRepo.findAllByIsActive(false);

        Set<Service> upserted = new LinkedHashSet<>();
        Set<Service> removed = new LinkedHashSet<>();
        List<Service> newList = new ArrayList<>();

        // add => find the same one in DB deactive list, the remaining will be created
        if (setupServiceRequest.getAddedServices() != null) {
            for (ServiceDTO dto : setupServiceRequest.getAddedServices()) {
                Service addedSer = deActives.stream()
                        .filter(s -> s.isTheSame(dto)).findFirst().orElse(null);
                if (addedSer != null) {
                    addedSer.setActive(true);
                    upserted.add(addedSer);
                } else {
                    newList.add(new Service(dto.getServiceName(), dto.getCost()));
                }
            }
        }
        // delete => find same one in DB active list, remaining will do nothing.
        if (setupServiceRequest.getDeletedServices() != null) {
            for (ServiceDTO dto : setupServiceRequest.getDeletedServices()) {
                actives.stream().filter(s -> s.isTheSame(dto)).findFirst().ifPresent(s -> {
                    s.setActive(false);
                    removed.add(s);
                });
            }
        }
        // save new list
        serviceRepo.saveAll(newList);
        // save existed list
        serviceRepo.saveAll(upserted);
        serviceRepo.saveAll(removed);

        // keep the inventory link and Giá bán on rows created here and on rows
        // just re-activated; a deleted row stops being sellable there too.
        syncServices(newList);
        syncServices(upserted);
        removed.forEach(this::deactivateServiceInInventory);
    }

    private void updateShuttleBalls(SetUpServiceRequest setupServiceRequest) {
        List<ShuttleBall> actives = shuttleRepo.findAllByIsActive(true);
        List<ShuttleBall> deActives = shuttleRepo.findAllByIsActive(false);

        Set<ShuttleBall> upserted = new LinkedHashSet<>();
        Set<ShuttleBall> removed = new LinkedHashSet<>();
        List<ShuttleBall> newList = new ArrayList<>();

        // add => find the same one in DB deactive list, the remaining will be created
        if (setupServiceRequest.getAddedShuttleBalls() != null) {
            for (ShuttleBallDTO dto : setupServiceRequest.getAddedShuttleBalls()) {
                ShuttleBall addedBall = deActives.stream()
                        .filter(b -> b.theSameDTO(dto)).findFirst().orElse(null);
                if (addedBall != null) {
                    addedBall.setActive(true);
                    upserted.add(addedBall);
                } else {
                    newList.add(new ShuttleBall(dto.getShuttleName(), dto.getShuttleCost()));
                }
            }
        }
        // delete => find same one in DB active list, remaining will do nothing.
        if (setupServiceRequest.getDeletedShuttleBalls() != null) {
            for (ShuttleBallDTO dto : setupServiceRequest.getDeletedShuttleBalls()) {
                actives.stream().filter(b -> b.theSameDTO(dto)).findFirst().ifPresent(b -> {
                    b.setActive(false);
                    removed.add(b);
                });
            }
        }
        // save new list
        shuttleRepo.saveAll(newList);
        // save existed list
        shuttleRepo.saveAll(upserted);
        shuttleRepo.saveAll(removed);

        syncBalls(newList);
        syncBalls(upserted);
        removed.forEach(ball -> catalogSync.deactivateCatalog(ball.getItem()));
    }

    /**
     * Mirrors catalog rows into inventory: creates the item on first use, links
     * it and copies the billing price into Giá bán. Rows are visited in name
     * order so a save can never take the same item rows as a concurrent page
     * load in the opposite order; a deadlock that still slips through is
     * retried instead of failing the save.
     */
    private void syncServices(Collection<Service> services) {
        services.stream()
                .sorted(Comparator.comparing(service -> sortKey(service.getSerName())))
                .forEach(service -> retryOnLockContention(
                        () -> catalogSync.linkService(service, BigDecimal.valueOf(service.getCost())),
                        () -> "to link service '" + service.getSerName() + "'"));
    }

    private void syncBalls(Collection<ShuttleBall> balls) {
        balls.stream()
                .sorted(Comparator.comparing(ball -> sortKey(ball.getShuttleName())))
                .forEach(ball -> retryOnLockContention(
                        () -> catalogSync.linkShuttleBall(ball, BigDecimal.valueOf(ball.getCost())),
                        () -> "to link shuttle ball '" + ball.getShuttleName() + "'"));
    }

    private void syncBall(ShuttleBall ball) {
        ShuttleBall saved = shuttleRepo.save(ball);
        retryOnLockContention(
                () -> catalogSync.linkShuttleBall(saved, BigDecimal.valueOf(saved.getCost())),
                () -> "to link shuttle ball '" + saved.getShuttleName() + "'");
    }

    private void deactivateServiceInInventory(Service service) {
        InventoryItem item = service.getItem() != null
                ? service.getItem()
                : serviceItemLinker.resolve(service);
        retryOnLockContention(
                () -> catalogSync.deactivateCatalog(item),
                () -> "to deactivate the stock item of '" + service.getSerName() + "'");
    }

    private static String sortKey(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /**
     * Read-path write, so it must never break the page: the heal is retried on
     * a fresh transaction and, if the rows stay locked, skipped for this load
     * (the page then renders the links that are already stored).
     */
    private void runQuietly(String action, Runnable work) {
        try {
            retryOnLockContention(work, () -> action);
        } catch (RuntimeException e) {
            log.warn("Skipped {} for this page load: {}", action, e.toString());
        }
    }

    private void retryOnLockContention(Runnable work, Supplier<String> action) {
        for (int attempt = 1;; attempt++) {
            try {
                work.run();
                return;
            } catch (RuntimeException e) {
                if (attempt >= LOCK_CONTENTION_RETRIES || !isLockContention(e)) {
                    throw e;
                }
                log.warn("Retrying {} after lock contention (attempt {})", action.get(), attempt);
                sleepBeforeRetry(attempt);
            }
        }
    }

    /**
     * Deadlock (MySQL 1213) / lock wait timeout (1205) anywhere in the cause
     * chain means the transaction was rolled back by the database, not that the
     * data is wrong — retrying on a fresh transaction succeeds.
     */
    private static boolean isLockContention(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof ConcurrencyFailureException
                    || current instanceof LockAcquisitionException) {
                return true;
            }
            if (current instanceof SQLException sql
                    && (sql.getErrorCode() == MYSQL_DEADLOCK || sql.getErrorCode() == MYSQL_LOCK_WAIT_TIMEOUT)) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep((long) RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private List<ShuttleBallDTO> findTheSameBallList(List<ShuttleBallDTO> originList, List<ShuttleBall> findList) {
        return originList.stream().filter(dto -> findList.stream().anyMatch(b -> b.theSameDTO(dto)))
                .collect(Collectors.toList());
    }

    private ShuttleBall findAddedShuttleBall(String shuttleName, float shuttleCost) {
        List<ShuttleBall> shuttleBalls = shuttleRepo.findAllByShuttleName(shuttleName);
        // not existed => create new one => return
        if (shuttleBalls.isEmpty()) {
            return new ShuttleBall(shuttleName, shuttleCost);
        }

        List<ShuttleBall> list = shuttleBalls.stream().filter(b -> !b.isActive()).collect(Collectors.toList());
        // new cost => return
        if (!list.isEmpty()) {
            list.get(0).setShuttleName(shuttleName);
            list.get(0).setCost(shuttleCost);
            list.get(0).setActive(true);
            return list.get(0);
        }
        // existing shuttle ball with the same name and cost => return null
        return new ShuttleBall(shuttleName, shuttleCost);
    }

    public Service findAddedService(String serviceName, float serviceCost) {
        if (!serviceName.isBlank()) {
            List<Service> dbService = serviceRepo.findAllBySerName(serviceName);
            if (dbService.isEmpty()) {
                return new Service(serviceName, serviceCost);
            }
            // Reuse service by getting the same name but inactive, including equals and not
            // equals cost
            List<Service> list = dbService.stream().filter(s -> !s.isActive()).collect(Collectors.toList());

            if (!list.isEmpty()) {
                list.get(0).setSerName(serviceName);
                list.get(0).setCost(serviceCost);
                list.get(0).setActive(true);
                return list.get(0);
            }
            return new Service(serviceName, serviceCost);
        }
        return null;
    }

    private int getMaxCourtNumber(List<Court> courts) {
        return courts.stream()
                .mapToInt(c -> {
                    try {
                        return Integer.parseInt(c.getCourtName().replace(COURT_STR, "").trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .max()
                .orElse(0);
    }

    private void checkAndCreateCourt(int totalCourt) {
        List<Court> activeCourts = courtRepo.findAllByIsActive(true);
        int currentActive = activeCourts.size();
        if (currentActive > totalCourt) {
            // Deactivate excess courts (highest numbered first)
            activeCourts.sort((a, b) -> {
                int numA = 0, numB = 0;
                try {
                    numA = Integer.parseInt(a.getCourtName().replace(COURT_STR, "").trim());
                } catch (NumberFormatException e) {
                }
                try {
                    numB = Integer.parseInt(b.getCourtName().replace(COURT_STR, "").trim());
                } catch (NumberFormatException e) {
                }
                return numB - numA;
            });
            int toDeactivate = currentActive - totalCourt;
            for (int i = 0; i < toDeactivate && i < activeCourts.size(); i++) {
                activeCourts.get(i).setActive(false);
            }
            courtRepo.saveAll(activeCourts);
            log.info("Deactivated {} courts.", toDeactivate);
        } else if (currentActive < totalCourt) {
            int toCreate = totalCourt - currentActive;
            int maxNum = getMaxCourtNumber(courtRepo.findAll());
            List<Court> newCourts = new ArrayList<>();
            for (int i = 1; i <= toCreate; i++) {
                newCourts.add(new Court(COURT_STR + (maxNum + i)));
            }
            courtRepo.saveAll(newCourts);
            log.info("Created {} courts.", toCreate);
        }
    }

    /**
     * Setup page view of the product catalog, enriched with the numbers owned by
     * Quản lý kho: remaining stock, Giá bán, unit and active state.
     *
     * @param includeInactive true so the page can show "Ngừng bán" rows and
     *                        restore them
     */
    @Override
    public SetUpServiceResponse getSetUpService(boolean includeInactive) {
        int totalCourt = courtRepo.findAllByIsActive(true).size();
        List<ShuttleBall> listShuttleBall = includeInactive
                ? shuttleRepo.findAll()
                : shuttleRepo.findAllByIsActive(true);
        List<Service> listService = includeInactive
                ? serviceRepo.findAll()
                : serviceRepo.findAllByIsActive(true);
        if (totalCourt == 0 && listShuttleBall.isEmpty() && listService.isEmpty()) {
            return null;
        }
        // rows created before the inventory feature carry no link; heal them so
        // the page shows the same numbers as Quản lý kho. A page load used to
        // rewrite every inventory row here, which made two concurrent requests
        // deadlock; both steps are now idempotent and must never fail the read.
        runQuietly("the catalog link heal", () -> {
            serviceItemLinker.resolveAllBalls(listShuttleBall);
            serviceItemLinker.resolveAll(listService);
        });
        // every sellable row must exist in inventory, otherwise the page would
        // show it as unlinked forever. Giá bán stored in Quản lý kho wins; a row
        // without an item seeds it from the billing price. Nothing is written
        // when the rows already match.
        runQuietly("the catalog sync", () -> catalogSync.ensureCatalogLinks(listShuttleBall, listService));

        SetUpServiceResponse result = new SetUpServiceResponse();
        result.setTotalCourt(totalCourt);
        result.setShuttleBalls(listShuttleBall.stream()
                .map(this::toShuttleBallResponse)
                .collect(Collectors.toList()));
        List<ServiceResponse> services = new ArrayList<>();
        for (Service service : listService) {
            if (ApiConstant.COST_IN_PERNSON.equals(service.getSerName())) {
                result.setCostInPerson(MoneyUtils.formatToVND(service.getCost()));
                continue;
            }
            if (ApiConstant.RENT_BY_TIME.equals(service.getSerName())) {
                result.setRentByTime(MoneyUtils.formatToVND(service.getCost()));
                continue;
            }
            services.add(toServiceResponse(service));
        }
        result.setServices(services);
        return result;
    }

    private ServiceResponse toServiceResponse(Service service) {
        ServiceResponse row = new ServiceResponse(service);
        InventoryItem item = service.getItem();
        if (item != null) {
            long stock = catalogSync.stockOnHand(item);
            row.setStockOnHand(stock);
            row.setLowStock(stock <= LOW_STOCK_THRESHOLD);
            row.setOutOfStock(stock <= 0);
            row.setStockBreakdown(item.packageBreakdown(stock));
        }
        return row;
    }

    private ShuttleBallResponse toShuttleBallResponse(ShuttleBall ball) {
        ShuttleBallResponse row = new ShuttleBallResponse(ball);
        InventoryItem item = ball.getItem();
        if (item != null) {
            long stock = catalogSync.stockOnHand(item);
            row.setStockOnHand(stock);
            row.setLowStock(stock <= LOW_STOCK_THRESHOLD);
            row.setOutOfStock(stock <= 0);
            row.setStockBreakdown(item.packageBreakdown(stock));
        }
        return row;
    }

    @Override
    public boolean deleteService(ServiceDTO serviceDTO) {
        List<Service> services = serviceRepo.findAllBySerName(serviceDTO.getServiceName());
        if (!services.isEmpty()) {
            Service equService = services.stream().filter(s -> s.getSerName().equals(serviceDTO.getServiceName()))
                    .findFirst().orElse(null);
            if (equService != null) {
                equService.setActive(false);
                boolean deleted = !serviceRepo.save(equService).isActive();
                if (deleted) {
                    deactivateServiceInInventory(equService);
                }
                return deleted;
            }
        }
        return false;
    }

    @Override
    public boolean deleteShuttleBall(ShuttleBallDTO shuttleBallDTO) {
        List<ShuttleBall> balls = shuttleRepo.findAllByShuttleName(shuttleBallDTO.getShuttleName());
        if (!balls.isEmpty()) {
            ShuttleBall equBall = balls.stream().filter(b -> b.isActive()).findFirst().orElse(null);
            if (equBall != null) {
                equBall.setActive(false);
                boolean deleted = !shuttleRepo.save(equBall).isActive();
                if (deleted) {
                    catalogSync.deactivateCatalog(equBall.getItem());
                }
                return deleted;
            }
        }
        return false;
    }
}
