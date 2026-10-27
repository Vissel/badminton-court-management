package com.badminton.service;

import com.badminton.BadmintonCourtManagementApplication;
import com.badminton.constant.CommonConstant;
import com.badminton.core.court.CoreCourtService;
import com.badminton.core.game.CoreGameService;
import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.core.inventory.ServiceItemLinker;
import com.badminton.entity.*;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.*;
import com.badminton.requestmodel.*;
import com.badminton.response.CourtManagementResponse;
import com.badminton.response.ServiceResponse;
import com.badminton.response.result.Result;
import com.badminton.service.calculator.GameExpenseCalculator;
import com.badminton.util.ServiceConverter;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@Slf4j
public class CourtServicesService {

    private final BadmintonCourtManagementApplication badmintonCourtManagementApplication;

    @Autowired
    private ServiceRepository serviceRepo;
    @Autowired
    private SessionServiceImpl session;
    @Autowired
    GameExpenseCalculator gameCalculator;
    @Autowired
    GameService gameService;
    @Autowired
    CoreGameService coreGameService;
    @Autowired
    ShuttleBallServiceImpl shuttleBallService;
    @Autowired
    private UserRepository userRepo;

    @Autowired
    CoreAvailablePlayerService coreAvailablePlayerService;

    @Autowired
    private ServiceTemplate serviceTemple;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    private AvailablePlayerRepository avaPlayerRepo;

    @Autowired
    private CourtRepositoty courtRepo;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ServiceItemLinker serviceItemLinker;
    @Autowired
    private CoreCourtService coreCourtService;

    private static final Long NULL_OF_LONG = -1L;
    private static final int FIRST = 0;
    private static final int LOW_STOCK_THRESHOLD = 10;

    CourtServicesService(BadmintonCourtManagementApplication badmintonCourtManagementApplication) {
        this.badmintonCourtManagementApplication = badmintonCourtManagementApplication;
    }

    @Transactional
    public List<ServiceResponse> getActiveServices() {
        List<Service> activeServices = serviceRepo.findAllByIsActive(true);
        return activeServices.stream().map(s -> {
            InventoryItem item = linkInventoryItem(s);
            ServiceResponse response = new ServiceResponse(s);
            enrichStock(response, item);
            return response;
        }).collect(Collectors.toList());
    }

    /**
     * Services created from Setup / product import are inserted without a stock
     * item
     * link, which silently disables stock tracking: no itemId on the picker (so the
     * client never asks for stock) and no RETAIL_SALE movement when one is dropped
     * on
     * a player. Heal the link from an active inventory item carrying the same name.
     */
    private InventoryItem linkInventoryItem(Service service) {
        return serviceItemLinker.resolve(service);
    }

    private void enrichStock(ServiceResponse response, InventoryItem item) {
        if (item == null) {
            return;
        }
        long stock = inventoryService.getStockOnHand(item);
        response.setStockOnHand(stock);
        response.setLowStock(stock <= LOW_STOCK_THRESHOLD);
    }

    /**
     * Get all active courts
     *
     * @return
     */
    public List<CourtDTO> getAllActiveCourts() {
        log.info("Service getAllActiveCourts {}", CommonConstant.START);
        List<Court> activeCourts = courtRepo.findAllByIsActive(true);
        log.debug("Size of active courts: {}", activeCourts.size());
        List<CourtDTO> courtDTOs = activeCourts.stream().map(court -> new CourtDTO(court)).collect(Collectors.toList());
        log.info("Service getAllActiveCourts {}", CommonConstant.END);
        return courtDTOs;
    }

    @Transactional
    public List<AvaPlayerDTO> getCurrentAvailablePlayers() {
        List<Session> activeSessions = session.findListCurrentSession();
        if (!activeSessions.isEmpty()) {
            return activeSessions.getFirst().getAvailablePlayers().stream().filter(ava -> ava.getLeaveTime() == null)
                    .map(ava -> new AvaPlayerDTO(ava)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * @return CourtManagement DTO include Game, Court, AvailablePlayer
     */
    @Transactional
    public CourtManagementResponse getCourtManagement() {
        log.info("Service getCourtManagement {}", CommonConstant.START);
        CourtManagementResponse res = new CourtManagementResponse();
        Set<Long> playerExcludes = initExcludeSet();
        Set<Integer> courtExcludes = initExcludeSet();
        try {
            log.info("Getting Available games.");
            List<Game> availableGames = gameService.findAllInprogress();
            log.debug("Size of Getting Available games:{}", availableGames.size());
            if (!availableGames.isEmpty()) {
                // set to gameDTOs result
                res.convertToGameDTOs(availableGames);
                // build the exclude list
                availableGames.forEach(g -> {
                    if (g.getTeamOne() != null) {
                        playerExcludes.add(getAvaIdFromPlayer(g.getTeamOne().getPlayerOne()));
                        playerExcludes.add(getAvaIdFromPlayer(g.getTeamOne().getPlayerTwo()));
                    }
                    if (g.getTeamTwo() != null) {
                        playerExcludes.add(getAvaIdFromPlayer(g.getTeamTwo().getPlayerOne()));
                        playerExcludes.add(getAvaIdFromPlayer(g.getTeamTwo().getPlayerTwo()));
                    }
                    courtExcludes.add(g.getCourt().getCourtId());
                });
                // remove the null Team player
                if (playerExcludes.size() > 1)
                    playerExcludes.remove(NULL_OF_LONG);
            }

            log.info("Getting current session.");
            List<Session> activeSessions = session.findListCurrentSession();
            if (!activeSessions.isEmpty()) {
                log.info("Getting list of Remain available players.");
                List<AvailablePlayer> listOfRemainAvaPlayer = avaPlayerRepo
                        .findAllBySessionAndAvaIdNotInAndLeaveTimeIsNull(activeSessions.getFirst(), playerExcludes);
                log.debug("Size of Remain available players:{}", listOfRemainAvaPlayer.size());
                res.convertToAvaPlayerDTOs(listOfRemainAvaPlayer);
                log.debug("Added Remain available players into result.");

                log.info("Getting list of Remain courts.");
                List<Court> listOfRemainCourt = courtRepo.findAllByIsActiveTrueAndCourtIdNotIn(courtExcludes);
                log.debug("Size of Remain courts:{}", listOfRemainCourt.size());
                res.convertToRemainCourtDTOs(listOfRemainCourt);
                log.debug("Added Remain courts into result.");
            }
        } catch (Exception e) {
            log.error("Error while getCourtManagement data:{}", e.getMessage());
        } finally {
            log.info("Service getCourtManagement {}", CommonConstant.END);
        }
        return res;
    }

    private Set initExcludeSet() {
        Set exSet = new HashSet<>();
        exSet.add(NULL_OF_LONG);
        return exSet;
    }

    public Result<Boolean> updatePlayerName(UpdatePlayerNameRequest request) {
        return serviceTemple.execute(new ProcessCallback<UpdatePlayerNameRequest, Boolean>() {
            @Override
            public UpdatePlayerNameRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(UpdatePlayerNameRequest request) {
                Assert.isTrue(StringUtils.isNotBlank(request.getCurrName()), "Current name must not be blank.");
                Assert.isTrue(StringUtils.isNotBlank(request.getNewName()), "New name must not be blank.");
                Assert.isTrue(isValidPlayerName(request.getNewName()), "New name contains invalid characters.");
            }

            @Override
            public Boolean process() throws BusinessException {
                return transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        return coreAvailablePlayerService.updateAvailablePlayerName(request.getCurrName(),
                                request.getNewName());
                    }
                });
            }
        });
    }

    public Result<Boolean> addPlayerToCurrentSession(AddPlayerRequest request) {
        return serviceTemple.execute(new ProcessCallback<AddPlayerRequest, Boolean>() {
            @Override
            public AddPlayerRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(AddPlayerRequest req) {
                Assert.notNull(req, "Request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(req.getPlayerName()), "Player name must not be blank.");
            }

            @Override
            public Boolean process() throws BusinessException {
                return transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        String name = request.getPlayerName()
                                .replace(CommonConstant.DOUBLE_QUOTES, CommonConstant.EMPTY).trim();
                        Float advanceAmount = request.getAdvanceAmount() != null ? request.getAdvanceAmount() : 0f;
                        return transactionAddPlayerToCurrentSessionWithAdvance(name, advanceAmount);
                    }
                });
            }
        });
    }

    public Result<Boolean> updateAvailablePlayer(AvaPlayerDTO avaPlayerDTO) {
        return serviceTemple.execute(new ProcessCallback<AvaPlayerDTO, Boolean>() {
            @Override
            public AvaPlayerDTO getRequest() {
                return avaPlayerDTO;
            }

            @Override
            public void preProcess(AvaPlayerDTO request) {
                Assert.notNull(request, "Player update request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(request.getOldPlayerName()),
                        "Current player name must not be blank.");
                Assert.isTrue(StringUtils.isNotBlank(request.getPlayerName()), "New player name must not be blank.");
            }

            @Override
            public Boolean process() throws BusinessException {
                return transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        return transactionUpdateAvailablePlayer(avaPlayerDTO);
                    }
                });
            }
        });
    }

    public Boolean transactionUpdateAvailablePlayer(AvaPlayerDTO avaPlayerDTO) {
        String oldPlayerName = avaPlayerDTO.getOldPlayerName()
                .replace(CommonConstant.DOUBLE_QUOTES, CommonConstant.EMPTY).trim();
        String newPlayerName = avaPlayerDTO.getPlayerName().replace(CommonConstant.DOUBLE_QUOTES, CommonConstant.EMPTY)
                .trim();
        Assert.isTrue(StringUtils.isNotBlank(oldPlayerName), "Current player name must not be blank.");
        Assert.isTrue(StringUtils.isNotBlank(newPlayerName), "New player name must not be blank.");

        AvailablePlayer availablePlayer = session.getAvailablePlayerInActiveSession(oldPlayerName);
        Assert.notNull(availablePlayer, "Available player does not exist in current session.");

        AvailablePlayer duplicatedAvailablePlayer = session.getAvailablePlayerInActiveSession(newPlayerName);
        Assert.isTrue(
                duplicatedAvailablePlayer == null || duplicatedAvailablePlayer.getAvaId() == availablePlayer.getAvaId(),
                "Available player has already been added.");

        Player player = availablePlayer.getPlayer();
        player.setPlayerName(newPlayerName);
        userRepo.save(player);
        return Boolean.TRUE;
    }

    public Boolean transactionAddPlayerToCurrentSessionWithAdvance(String name, Float advanceAmount) {
        // try {
        List<Player> listPlayer = userRepo.findAllByPlayerName(name);

        Player player = null;
        boolean update = false;
        if (!listPlayer.isEmpty()) {
            player = listPlayer.getFirst();
            // set new name in case of mismatch upper/lower cases
            if (!player.getPlayerName().equals(name)) {
                player.setPlayerName(name);
                update = true;
            }
        } else {
            player = new Player(name, name);
            update = true;
        }

        if (update) {
            userRepo.save(player);
        }
        if (player != null && player.getPlayerId() != 0) {

            // add to current session
            Session currSession = session.findListCurrentSession().getFirst();
            Assert.notNull(currSession, "Not available session.");

            List<AvailablePlayer> availablePlayerList = avaPlayerRepo
                    .findAllBySessionAndPlayerAndLeaveTimeIsNull(currSession, player);
            Assert.isTrue(availablePlayerList.isEmpty(), "Available player has already been added.");
            AvailablePlayer newAvaPlayer = new AvailablePlayer(player, currSession);

            // Handle advance payment
            if (advanceAmount != null && advanceAmount > 0) {
                newAvaPlayer.setAdvancePayment(advanceAmount);
                // Add "Trả trước" as a negative-cost service line item
                ServiceDTO advanceServiceDTO = new ServiceDTO();
                advanceServiceDTO.setServiceName(com.badminton.constant.GameConstant.ADVANCE_PAYMENT_VN);
                advanceServiceDTO.setCost(advanceAmount);
                newAvaPlayer.setServices(
                        ServiceUtil.addServiceToJsonArray(newAvaPlayer.getCurrentServices(), advanceServiceDTO));
            }

            avaPlayerRepo.save(newAvaPlayer);
        }
        // } catch (IllegalArgumentException e) {
        // throw new BusinessException(ErrorCodeEnum.FLOW_ERROR, "");
        // }
        return Boolean.TRUE;
    }

    /**
     * Add service to available player
     *
     * @param playerName
     * @return
     */
    @Transactional
    public Boolean addServiceToAvailablePlayer(ServiceRequest serviceRequest, String playerName) {
        AvailablePlayer availablePlayer = session.getAvailablePlayerInActiveSession(playerName);
        if (availablePlayer != null) {
            ServiceDTO serviceDTO = ServiceConverter.convertRequestToDTO(serviceRequest);
            stampStockItem(serviceDTO);
            availablePlayer
                    .setServices(ServiceUtil.addServiceToJsonArray(availablePlayer.getCurrentServices(), serviceDTO));
            avaPlayerRepo.save(availablePlayer);
            inventoryService.recordRetailSales(List.of(serviceDTO), availablePlayer.getAvaId());
            return true;
        }
        return false;
    }

    /**
     * Put the resolved stock item and the sold quantity on the line before it is
     * stored in the player's JSON. Without this the line keeps
     * {@code itemId = null}, so the ledger can only resolve the item by name and
     * later add/remove deltas compare a name key against an id key - which never
     * cancel out and double the movements.
     */
    private void stampStockItem(ServiceDTO serviceDTO) {
        if (serviceDTO.getQuantity() == null || serviceDTO.getQuantity() <= 0) {
            serviceDTO.setQuantity(1);
        }
        if (serviceDTO.getItemId() != null) {
            return;
        }
        serviceRepo.findBySerName(serviceDTO.getServiceName())
                .map(serviceItemLinker::resolve)
                .ifPresent(item -> serviceDTO.setItemId(item.getItemId()));
    }

    /**
     * Remove service out available player
     *
     * @param serviceDTO
     * @param playerName
     * @return
     */
    @Transactional
    public Boolean removeServiceOutAvailablePlayer(ServiceDTO serviceDTO, String playerName) {
        AvailablePlayer availablePlayer = session.getAvailablePlayerInActiveSession(playerName);
        if (availablePlayer != null) {
            availablePlayer.setServices(
                    ServiceUtil.divideServiceFromJsonArray(availablePlayer.getCurrentServices(),
                            serviceDTO));
            // ServiceUtil.divideService(availablePlayer.getCurrentServices(),
            // ServiceUtil.buildService(serviceDTO.getServiceName(),
            // serviceDTO.getCost())));
            avaPlayerRepo.save(availablePlayer);
            inventoryService.recordServiceReturns(List.of(serviceDTO), availablePlayer.getAvaId());
            return true;
        }
        return false;
    }

    /**
     * update new list service to available player
     *
     * @param playerName
     * @return
     */
    @Transactional
    public Boolean updateServicesToAvailablePlayer(List<ServiceRequest> listServiceRequest, String playerName) {
        AvailablePlayer availablePlayer = session.getAvailablePlayerInActiveSession(playerName);
        if (availablePlayer != null) {
            List<ServiceDTO> listServiceDTO = listServiceRequest.stream()
                    .map(req -> ServiceConverter.convertRequestToDTO(req))
                    .collect(Collectors.toList());
            listServiceDTO.forEach(this::stampStockItem);
            List<ServiceDTO> previous = ServiceUtil.convertStringToListService(availablePlayer.getCurrentServices());
            recordServiceDeltas(previous, listServiceDTO, availablePlayer.getAvaId());
            String listServiceString = ServiceUtil.buildJsonArrayStr(listServiceDTO);
            // listServiceDTO.stream().map(serviceDTO ->
            // ServiceUtil.buildJsonService(serviceDTO))
            // .collect(Collectors.joining(CommonConstant.STR_SEMI_COLON));
            availablePlayer.setServices(listServiceString);
            avaPlayerRepo.save(availablePlayer);
            return true;
        }
        return false;
    }

    /**
     * Net the previous service list against the new one and emit RETAIL_SALE
     * / RETURN movements for the difference per stockable item.
     */
    private void recordServiceDeltas(List<ServiceDTO> previous, List<ServiceDTO> current, Long refId) {
        Map<String, Integer> net = new HashMap<>();
        for (ServiceDTO dto : previous) {
            net.merge(serviceDeltaKey(dto), dto.getQuantity() != null && dto.getQuantity() > 0 ? dto.getQuantity() : 1,
                    Integer::sum);
        }
        for (ServiceDTO dto : current) {
            net.merge(serviceDeltaKey(dto),
                    -(dto.getQuantity() != null && dto.getQuantity() > 0 ? dto.getQuantity() : 1), Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : net.entrySet()) {
            int delta = entry.getValue();
            if (delta == 0) {
                continue;
            }
            ServiceDTO dto = keyToDto(entry.getKey());
            dto.setQuantity(Math.abs(delta));
            if (delta > 0) {
                inventoryService.recordServiceReturns(List.of(dto), refId);
            } else {
                inventoryService.recordRetailSales(List.of(dto), refId);
            }
        }
    }

    private String serviceDeltaKey(ServiceDTO dto) {
        if (dto.getItemId() != null) {
            return "ID:" + dto.getItemId();
        }
        // Lines stored before the inventory link existed only carry a name; resolve
        // them to the same id key so an unchanged line nets to zero.
        InventoryItem item = serviceItemLinker.findByServiceName(dto.getServiceName());
        return item != null ? "ID:" + item.getItemId() : dto.getServiceName();
    }

    private ServiceDTO keyToDto(String key) {
        ServiceDTO dto = new ServiceDTO();
        if (key.startsWith("ID:")) {
            dto.setItemId(Integer.valueOf(key.substring(3)));
        } else {
            dto.setServiceName(key);
        }
        return dto;
    }

    /**
     * Determine team for player based on court area \n team 1:[player1, player2] =
     * [area_A,area_B] \n team 2:[player1, player2] = [area_C,area_D] \n
     *
     * <p>
     * The game/team rows belong to the court core, so this only delegates.
     * </p>
     */
    public Boolean addAvailablePlayerToCourtArea(String playerName, CourtDTO courtDTO, ShuttleBallDTO shuttleBallDTO) {
        return coreCourtService.addAvailablePlayerToCourtArea(playerName, courtDTO, shuttleBallDTO);
    }

    private long getAvaIdFromPlayer(AvailablePlayer avaPlayer) {
        if (avaPlayer != null) {
            return avaPlayer.getAvaId();
        }
        return NULL_OF_LONG;
    }

    private boolean isValidPlayerName(String name) {
        // Reject only harmful characters: script tags and control characters
        // Allow all other normal characters including quotes, hyphens, etc.
        String harmfulPattern = ".*[<>\\x00-\\x1F].*";
        return !name.matches(harmfulPattern);
    }

    /**
     * Free the requested court area; the court core owns the game/team rows and
     * drops the court's
     * game once no player is left on it.
     */
    public Boolean removeAvailablePlayerFromCourtArea(CourtDTO courtDTO) {
        return coreCourtService.removeAvailablePlayerFromCourtArea(courtDTO);
    }

    @Transactional
    public Result<Boolean> removeAvaPlayerOutSession(String playerName) {
        return serviceTemple.execute(new ProcessCallback<String, Boolean>() {
            @Override
            public String getRequest() {
                return playerName;
            }

            @Override
            public void preProcess(String request) {
                Assert.isTrue(StringUtils.isNotBlank(request), "Name must not be blank.");
            }

            @Override
            public Boolean process() throws BusinessException {
                return transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        return session.removePlayerOutCurrentSession(
                                playerName.replace(CommonConstant.DOUBLE_QUOTES, CommonConstant.EMPTY).trim());
                    }
                });
            }
        });
    }

    public Boolean isCourtReadyForRent(int courtId) {
        return courtRepo.findById(courtId).map(court -> {
            return coreGameService.isGameNotStartInCourt(court);
        }).orElse(false);
    }
}
