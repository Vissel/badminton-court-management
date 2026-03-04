package com.badminton.service;

import com.badminton.BadmintonCourtManagementApplication;
import com.badminton.constant.CommonConstant;
import com.badminton.constant.GameState;
import com.badminton.entity.*;
import com.badminton.exception.BusinessException;
import com.badminton.exception.ElementNotExistException;
import com.badminton.exception.ErrorMess;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.*;
import com.badminton.requestmodel.*;
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
public class CourtServicesServiceImpl {

    private final BadmintonCourtManagementApplication badmintonCourtManagementApplication;

    @Autowired
    private ServiceRepositoty serviceRepo;
    @Autowired
    private SessionServiceImpl session;
    @Autowired
    GameExpenseCalculator gameCalculator;
    @Autowired
    GameService gameService;
    @Autowired
    ShuttleBallServiceImpl shuttleBallService;
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ServiceTemple serviceTemple;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    private AvailablePlayerRepository avaPlayerRepo;

    @Autowired
    private GameRepository gameRepo;
    @Autowired
    private ShuttleBallRepositoty ballRepo;
    @Autowired
    private CourtRepositoty courtRepo;
    @Autowired
    private TeamRepository teamRepo;

    private static final Long NULL_OF_LONG = -1L;
    private static final int FIRST = 0;

    CourtServicesServiceImpl(BadmintonCourtManagementApplication badmintonCourtManagementApplication) {
        this.badmintonCourtManagementApplication = badmintonCourtManagementApplication;
    }

    public List<ServiceResponse> getActiveServices() {
        List<Service> activeServices = serviceRepo.findAllByIsActive(true);
        return activeServices.stream().map(s -> new ServiceResponse(s)).collect(Collectors.toList());
    }

    @Transactional
    public List<AvaPlayerDTO> getCurrentAvailablePlayers() {
        List<Session> activeSessions = session.findListCurrentSession();
        if (!activeSessions.isEmpty()) {
            return activeSessions.getFirst().getAvailablePlayers().stream().filter(ava -> ava.getLeaveTime() == null).map(ava -> new AvaPlayerDTO(ava)).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * @return CourtManagement DTO include Game, Court, AvailablePlayer
     */
    @Transactional
    public CourtManagementDTO getCourtManagement() {
        log.info("Service getCourtManagement {}", CommonConstant.START);
        CourtManagementDTO res = new CourtManagementDTO();
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
                if (playerExcludes.size() > 1) playerExcludes.remove(NULL_OF_LONG);
            }

            log.info("Getting current session.");
            List<Session> activeSessions = session.findListCurrentSession();
            if (!activeSessions.isEmpty()) {
                log.info("Getting list of Remain available players.");
                List<AvailablePlayer> listOfRemainAvaPlayer =
                        avaPlayerRepo.findAllBySessionAndAvaIdNotInAndLeaveTimeIsNull(activeSessions.getFirst(), playerExcludes);
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

    public Result<Boolean> addPlayerToCurrentSession(String name) {
        return serviceTemple.execute(new ProcessCallback<String, Boolean>() {
            @Override
            public String getRequest() {
                return name;
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
                        return transactionAddPlayerToCurrentSession(name.replace(CommonConstant.DOUBLE_QUOTES, CommonConstant.EMPTY).trim());
                    }
                });
            }
        });
    }

    //    @Transactional(rollbackFor = {BusinessException.class, Exception.class})
    public Boolean transactionAddPlayerToCurrentSession(String name) {
//        try {
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

            List<AvailablePlayer> availablePlayerList = avaPlayerRepo.findAllBySessionAndPlayerAndLeaveTimeIsNull(currSession, player);
            Assert.isTrue(availablePlayerList.isEmpty(), "Available player has already been added.");
            avaPlayerRepo.save(new AvailablePlayer(player, currSession));
        }
//        } catch (IllegalArgumentException e) {
//            throw new BusinessException(ErrorCodeEnum.FLOW_ERROR, "");
//        }
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
            availablePlayer.setServices(ServiceUtil.addServiceToJsonArray(availablePlayer.getCurrentServices(), serviceDTO));
            avaPlayerRepo.save(availablePlayer);
            return true;
        }
        return false;
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
//                    ServiceUtil.divideService(availablePlayer.getCurrentServices(),
//                    ServiceUtil.buildService(serviceDTO.getServiceName(), serviceDTO.getCost())));
            avaPlayerRepo.save(availablePlayer);
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
            List<ServiceDTO> listServiceDTO = listServiceRequest.stream().map(req -> ServiceConverter.convertRequestToDTO(req))
                    .collect(Collectors.toList());
            String listServiceString = ServiceUtil.buildJsonArrayStr(listServiceDTO);
//                    listServiceDTO.stream().map(serviceDTO ->
//                            ServiceUtil.buildJsonService(serviceDTO))
//                    .collect(Collectors.joining(CommonConstant.STR_SEMI_COLON));
            availablePlayer.setServices(listServiceString);
            avaPlayerRepo.save(availablePlayer);
            return true;
        }
        return false;
    }

    /**
     * Determine team for player based on court area \n team 1:[player1, player2] =
     * [area_A,area_B] \n team 2:[player1, player2] = [area_C,area_D] \n
     *
     * @param playerName
     * @param courtDTO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addAvailablePlayerToCourtArea(String playerName, CourtDTO courtDTO, ShuttleBallDTO shuttleBallDTO) throws Exception {
        log.info(ErrorMess.ADD_PLAYER_INTO_COURT, CommonConstant.START);
        // create game if there is new, update otherwise
        Optional<Court> courtOpt = courtRepo.findById(Integer.valueOf(courtDTO.getCourtId()));
        if (!courtOpt.isPresent()) {
            return false;
        }

        Optional<Game> gameOfCourtOpt = gameRepo.findByCourtAndStateAndEndedDateIsNull(courtOpt.get(), GameState.NOT_START.getValue());
        Game game = null;
        if (gameOfCourtOpt.isPresent()) {
            game = gameOfCourtOpt.get();
            if (!GameState.NOT_START.getValue().equals(game.getState())) {
                log.error(ErrorMess.GAME_STATE_ERROR, courtDTO.getCourtName(), GameState.NOT_START, game.getState());
                return false;
            }
        } else {
            List<ShuttleBall> balls = ballRepo.findAllByShuttleNameAndCostAndIsActiveTrue(shuttleBallDTO.getShuttleName(), shuttleBallDTO.getShuttleCost());

            if (balls.isEmpty()) {
                log.error("No finding shuttleName:{} with cost:{}", shuttleBallDTO.getShuttleName(), shuttleBallDTO.getShuttleCost());
                return false;
            }

            // create new game
            game = new Game(courtOpt.get(), balls.get(0));
            gameRepo.save(game);
        }


        // create team 1 or 2 if there is new, update otherwise
        return addAvailablePlayerIntoGame(game, playerName, courtDTO.getCourtAreas().getFirst().getArea());
    }

    @Transactional
    private boolean addAvailablePlayerIntoGame(Game game, String playerName, String area) {
        try {
            Team team = getTeam(game, area);

            Optional<AvailablePlayer> optPlayer = avaPlayerRepo.findAvailablePlayerInSessionByName(session.findListCurrentSession().getFirst(), playerName);
            if (!optPlayer.isPresent()) {
                throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND, "Available player is not found.");
            }
            AvailablePlayer player = optPlayer.get();
            // Determine team for
            switch (area) {
                case GameState.Player.PLAYER_A, GameState.Player.PLAYER_C:
                    team.setPlayerOne(player);
                    log.info("added {} to player one.", playerName);
                    break;
                case GameState.Player.PLAYER_B, GameState.Player.PLAYER_D:
                    team.setPlayerTwo(player);
                    log.info("added {} to player two.", playerName);
                    break;
                default:
            }
            boolean updateGame = team.getTeamId() == 0;
            teamRepo.save(team);
            log.info("Saved {} to tbl_team DB.", playerName);
            // update new team to game
            // game.getTeamOne is null or team.getTeamTwo is null
            if (updateGame) {

                if (areaOfTeamOne(area)) {
                    game.setTeamOne(team);
                } else {
                    game.setTeamTwo(team);
                }
                gameRepo.save(game);
                log.info("Saved {} to tbl_game DB.", team.getTeamId());
            }

            return true;
        } catch (NullPointerException e) {
            log.error(ErrorMess.ADD_PLAYER_INTO_COURT, " has null exception:{}", e.getMessage());
        } catch (BusinessException e) {
            log.error(e.getErrorCodeEnum().getName());
        } finally {
            log.info(ErrorMess.ADD_PLAYER_INTO_COURT, CommonConstant.END);
        }
        return false;
    }

    /**
     * Req5 - Change game state: Started, Finish, Cancel <br>
     * Flows: Not start -> Started <br>
     * Started -> Finish <br>
     * Started -> Cancel
     *
     * @return
     */
    public Boolean changeGameState(GameDTO gameDTO) {
        try {
            log.info("Changing GameState {}", CommonConstant.START);
            final String stateChange = gameDTO.getGameState();
            final int courtId = Integer.valueOf(gameDTO.getCourt().getCourtId());
            Optional<Game> gameOpt = gameRepo.findByCourtIdAndEndedDateIsNull(courtId);
            if (!gameOpt.isPresent()) {
                throw new ElementNotExistException(ErrorCodeEnum.GAME_NOT_FOUND, String.format("No game is found by courtId [%s]", courtId));
            }
            Game game = gameOpt.get();
            GameState currentGameState = GameState.getGameState(game.getState());
            GameState changeGameState = GameState.getGameState(stateChange);
            if (currentGameState != null && changeGameState != null) {
                boolean validGameState = ServiceUtil.validGameStateUpdate(currentGameState, changeGameState);
                boolean isStartGame = readyToStart(changeGameState, game.getTeamOne(), game.getTeamTwo());
                // update
                if (validGameState && isStartGame) {
                    game.setState(stateChange);
                    setSelectedBallIntoGame(game, gameDTO.getShuttleBalls(), stateChange);
                    // update ended time for FINISH & CANCEL state
                    if (ServiceUtil.isEndedState(changeGameState)) {
                        game.setEndedDate(session.getUTCPlus7Instant());
                        // calculate and save the expense of game.
                        gameCalculator.calculateGameResult(game);
                    }
                    gameRepo.save(game);
                    return true;
                }

            }
        } catch (NumberFormatException | BusinessException e) {
            log.error("Court id [{}] is invalid.{}", gameDTO.getCourt().getCourtId(), e.getMessage());
        } catch (Exception e) {
            log.error("Error:{}", e.getMessage());
        } finally {
            log.info("Changing GameState {}", CommonConstant.END);
        }
        return false;
    }

    private void setSelectedBallIntoGame(Game game, List<ShuttleBallDTO> shuttleBalls, String gameStateChange) {
        if (GameState.START.equals(gameStateChange)) {
            game.setShuttleMap(Arrays.asList(shuttleBallService.createGameShuttleMap(game, shuttleBalls.getFirst(), shuttleBalls.getFirst().getBallQuantity())));
        }
    }

    private boolean readyToStart(GameState changeGameState, Team teamOne, Team teamTwo) {
        if (changeGameState.equals(GameState.START)) {
            boolean teamOneReady = teamOne.getPlayerOne() != null || teamOne.getPlayerTwo() != null;
            boolean teamTwoReady = teamTwo.getPlayerOne() != null || teamTwo.getPlayerTwo() != null;
            return teamOneReady && teamTwoReady;
        }
        return true;
    }

    private Team getTeam(Game game, String area) {
        Team team = null;
        if (game.getTeamOne() != null && areaOfTeamOne(area)) {
            team = game.getTeamOne();
            log.info("Team one...");
        } else if (game.getTeamTwo() != null && !areaOfTeamOne(area)) {
            team = game.getTeamTwo();
            log.info("Team two...");
        } else {
            team = new Team();
            team.setGame(game);
            log.info("Init new Team...");
        }
        return team;
    }

    private boolean areaOfTeamOne(String area) {
        boolean isTeamOne = true;
        // Determine team for
        switch (area) {
            case GameState.Player.PLAYER_A, GameState.Player.PLAYER_B:
                break;
            case GameState.Player.PLAYER_C, GameState.Player.PLAYER_D:
                isTeamOne = false;
                break;
            default:

        }
        return isTeamOne;
    }


    private long getAvaIdFromPlayer(AvailablePlayer avaPlayer) {
        if (avaPlayer != null) {
            return avaPlayer.getAvaId();
        }
        return NULL_OF_LONG;
    }

    public Boolean removeAvailablePlayerFromCourtArea(CourtDTO courtDTO) {

        // 1. Get game by courtid
        Optional<Game> gameOpt = gameRepo.findByCourtIdAndEndedDateIsNull(Integer.valueOf(courtDTO.getCourtId()));
        if (!gameOpt.isPresent() || !GameState.NOT_START.getValue().equals(gameOpt.get().getState())) {
            return Boolean.FALSE;
        }

        // 2. Determine Team by areaKey && Remove player from team.
        Team team;
        final String area = courtDTO.getCourtAreas().getFirst().getArea();
        switch (area) {
            case GameState.Player.PLAYER_A, GameState.Player.PLAYER_B:
                team = gameOpt.get().getTeamOne();
                if (GameState.Player.PLAYER_A.equals(area)) {
                    team.setPlayerOne(null);
                } else {
                    team.setPlayerTwo(null);
                }
                break;
            case GameState.Player.PLAYER_C, GameState.Player.PLAYER_D:
                team = gameOpt.get().getTeamTwo();
                if (GameState.Player.PLAYER_C.equals(area)) {
                    team.setPlayerOne(null);
                } else {
                    team.setPlayerTwo(null);
                }
                break;
            default:
        }
        // 3. Save to DB
        gameRepo.save(gameOpt.get());
        return Boolean.TRUE;
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
                        return session.removePlayerOutCurrentSession(playerName.replace(CommonConstant.DOUBLE_QUOTES, CommonConstant.EMPTY).trim());
                    }
                });
            }
        });
    }
}
