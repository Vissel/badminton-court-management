package com.badminton.service.impl;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.core.game.CoreGameService;
import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.ShuttleBall;
import com.badminton.entity.Team;
import com.badminton.exception.BusinessException;
import com.badminton.exception.ErrorUtils;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.model.game.*;
import com.badminton.model.inventory.GameEndConfirmDTO;
import com.badminton.repository.GameRepository;
import com.badminton.requestmodel.CourtAreaDTO;
import com.badminton.requestmodel.CourtDTO;
import com.badminton.requestmodel.GameDTO;
import com.badminton.requestmodel.inventory.BallConsumeInGameRequest;
import com.badminton.requestmodel.rent.EndRentGameRequest;
import com.badminton.response.result.*;
import com.badminton.service.*;
import com.badminton.service.calculator.GameExpenseCalculator;
import com.badminton.util.CommonUtil;
import com.badminton.util.MoneyUtils;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class GameServiceImpl implements GameService {

    @Autowired
    GameRepository gameRepository;
    @Autowired
    GameExpenseCalculator gameExpenseCalculator;
    @Autowired
    ServiceTemplate serviceTemple;
    @Autowired
    ShuttleBallServiceImpl shuttleBallService;
    @Autowired
    CoreGameService coreGameService;
    @Autowired
    InventoryService inventoryService;
    @Autowired
    PayService payService;
    @Autowired
    TransactionTemplate transactionTemplate;

    @Transactional
    @Override
    public List<Game> findAllInprogress() {
        return gameRepository.findAllByStateInAndEndedDateIsNull(getNonFinishGameState());
    }

    private Set<String> getNonFinishGameState() {
        return new HashSet<>(Arrays.asList(GameState.NOT_START.getValue(), GameState.START.getValue()));
    }

    /**
     * Get a GameResult when user click on Finish btn.
     * <br> In this time, get the shuttle ball number and quantityNot calculate the money yet. </br>
     * <br> Front-end will base on money, ball's  quantity to calculate.
     * <br> Human verify, do adjustment and   click confirm after that >> next api </br>
     *
     * @param courtId
     * @return
     */
    @Override
    public GameResult getGameResult(int courtId) {
        GameResult result = new GameResult();
        Optional<Game> optGame = gameRepository.findByCourtIdAndEndedDateIsNull(courtId);
        if (optGame.isPresent()) {
            Game startedGame = optGame.get();
            result.setGameId(startedGame.getGameId());
            result.setCourtResult(new CourtResult(startedGame.getCourt().getCourtId(), startedGame.getCourt().getCourtName()));
            final Map<ShuttleBallResponse, Integer> ballResMap = ServiceUtil.retrievedShuttleBallMap(startedGame.getShuttleMap());
            result.setBallResultMap(ballResMap);
            result.setTeamOneResult(buildTeamResult(startedGame.getTeamOne(), ballResMap, startedGame));
            result.setTeamTwoResult(buildTeamResult(startedGame.getTeamTwo(), ballResMap, startedGame));
            result.setCreatedDate(startedGame.getCreatedDate());
            result.setEndedDate(startedGame.getEndedDate());
            result.setState(startedGame.getState());
        }
        return result;
    }

    /**
     * Req5 - Centralized game state transition: Start, Finish, Cancel <br>
     * Flows: Not start -> Started <br>
     * Started -> Finish <br>
     * Started -> Cancel
     */
    @Override
    public Result<Boolean> handleChangeGameState(GameDTO gameRequest) {
        return serviceTemple.execute(new ProcessCallback<GameDTO, Boolean>() {

            @Override
            public GameDTO getRequest() {
                return gameRequest;
            }

            @Override
            public void preProcess(GameDTO request) {
                // 1. validate general game info
                validateChangeGameStateRequest(request);
            }

            @Override
            public Boolean process() throws BusinessException {
                return transactionTemplate.execute(new TransactionCallback<Boolean>() {
                    @Override
                    public Boolean doInTransaction(TransactionStatus status) {
                        try {
                            // 2. get Game
                            Game game = coreGameService.findActiveGame(gameRequest.getCourt().getCourtId());
                            // 3. branch the game by change state
                            GameState changeGameState = GameState.getGameState(gameRequest.getGameState());
                            return switch (changeGameState) {
                                case START -> isRentGameType(gameRequest.getGameType())
                                        ? processStartRentGame(game, gameRequest)
                                        : processStartGame(game, gameRequest);
                                default ->
                                        throw new RuntimeException(ErrorUtils.buildErrorMessage(ErrorCodeEnum.GAME_STATE_ERROR));
                            };
                        } catch (BusinessException e) {
                            throw new RuntimeException(ErrorUtils.buildErrorMessage(ErrorCodeEnum.FLOW_ERROR));
                        }

                    }
                });

            }
        });
    }

    @Override
    public Result<Boolean> handleFinishGame(GameDTO gameRequest) {
        return serviceTemple.execute(new ProcessCallback<GameDTO, Boolean>() {

            @Override
            public GameDTO getRequest() {
                return gameRequest;
            }

            @Override
            public void preProcess(GameDTO request) {
                // 1. validate Game confirmation value
                validateGameFinishField(gameRequest);
            }

            @Override
            public Boolean process() throws BusinessException {
                GameModel gameModel = calculateGameExpensetoGameModel(gameRequest);
                String courtId = gameModel.getCourtModel().getCourtId();
                // Snapshot the active game's mutable fields BEFORE finishing, so the finish can be
                // reverted by gameId later (once endedDate is set the row is no longer "active"
                // and can't be found by court lookup).
                GameSnapshot snapshot = coreGameService.snapshotActiveGame(courtId);
                boolean confirmSucceeded = false;
                try {
                    // 1. delegate finish logic to CoreGameService (commits in its own transaction)
                    coreGameService.finishGame(gameModel);
                    // 2. confirm in the inventory (its own transaction; rolls back on error)
                    Result<Boolean> confirmResult = inventoryService.confirmMovementItemEndGame(
                            toGameEndConfirmDTO(courtId, true));
                    if (confirmResult == null || !confirmResult.isSuccess()) {
                        throw new BusinessException(ErrorCodeEnum.FLOW_ERROR,
                                "Inventory confirmation failed for the finished game.");
                    }
                    confirmSucceeded = true;
                    return true;
                } finally {
                    // confirmMovementItemEndGame threw/rolled back its own transaction, but the
                    // finishGame write already committed - revert the Game to keep them consistent.
                    if (!confirmSucceeded) {
                        coreGameService.revertFinishedGame(snapshot);
                    }
                }
            }
        });
    }

    @Override
    public Result<Boolean> terminateGame(GameDTO gameRequest) {
        return serviceTemple.execute(new ProcessCallback<GameDTO, Boolean>() {
            @Override
            public GameDTO getRequest() {
                return gameRequest;
            }

            @Override
            public void preProcess(GameDTO request) {
                Assert.isTrue(CommonUtil.isNotNullEmpty(request.getCourt().getCourtId()), "CourtId must not be null or empty");
                try {
                    Integer.valueOf(request.getCourt().getCourtId());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid CourtId:" + request.getCourt().getCourtId());
                }
            }

            @Override
            public Boolean process() throws BusinessException {
                GameModel gameModel = toGameModelForTerminate(gameRequest);
                String courtId = gameModel.getCourtModel().getCourtId();
                // Capture the active game's identity + prior state BEFORE cancelling, so the
                // cancel can be reverted by gameId later (once endedDate is set the row is no
                // longer "active" and can't be found by court lookup).
                Game activeGame = coreGameService.findActiveGame(courtId);
                int gameId = activeGame.getGameId();
                String previousState = activeGame.getState();
                boolean confirmSucceeded = false;
                try {
                    // 1. delegate terminate logic to CoreGameService (commits in its own transaction)
                    coreGameService.cancelGame(gameModel);
                    // 2. confirm in the inventory (its own transaction; rolls back on error)
                    Result<Boolean> confirmResult = inventoryService.confirmMovementItemEndGame(
                            toGameEndConfirmDTO(courtId, false));
                    if (confirmResult == null || !confirmResult.isSuccess()) {
                        throw new BusinessException(ErrorCodeEnum.FLOW_ERROR,
                                "Inventory confirmation failed for the terminated game.");
                    }
                    confirmSucceeded = true;
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException(ErrorUtils.buildErrorMessage(ErrorCodeEnum.FLOW_ERROR), e);
                } finally {
                    // confirmMovementItemEndGame threw/rolled back its own transaction, but the
                    // cancelGame write already committed - revert the Game status to keep them consistent.
                    if (!confirmSucceeded) {
                        coreGameService.revertCancelledGame(gameId, previousState);
                    }
                }
            }
        });
    }

    @Override
    public List<String> findOpenGameStates(int courtId) {
        return gameRepository.findAllByCourtIdAndEndedDateIsNull(courtId).stream()
                .map(Game::getState)
                .toList();
    }

    @Override
    public Result<Boolean>
    endRentGame(EndRentGameRequest request) {
        return serviceTemple.execute(new ProcessCallback<EndRentGameRequest, Boolean>() {

            @Override
            public EndRentGameRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(EndRentGameRequest req) {
                Assert.notNull(req, "End rent game request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(req.getCourtId()), "Court id must not be empty.");
                GameState targetState = toEndGameState(req.getGameState());
                Assert.notNull(targetState, "Invalid game state: " + req.getGameState());
                Assert.isTrue(ServiceUtil.isEndedState(targetState),
                        "Only Finish or Cancel can end a rent game.");
            }

            @Override
            public Boolean process() throws BusinessException {
                return coreGameService.endRentGame(request.getCourtId(),
                        toEndGameState(request.getGameState()));
            }
        });
    }

    private GameState toEndGameState(String gameState) {
        return StringUtils.isBlank(gameState) ? null : GameState.getGameState(gameState);
    }

    @Override
    public Boolean saveAll(List<Game> gameList) {
        gameRepository.saveAll(gameList);
        return Boolean.TRUE;
    }

    @Override
    public Boolean checkGameExistById(int id) {
        return coreGameService.checkGameExistById(id);
    }

    private TeamResult buildTeamResult(Team team, Map<ShuttleBallResponse, Integer> shuttleMap, Game startedGame) {
        TeamResult teamResult = new TeamResult();
        Float totalBallCost = gameExpenseCalculator.getTotalBallCost(shuttleMap);

//        teamResult.setWin(ServiceUtil.getGameStatus(team.isWin()));
        if (ServiceUtil.availablePlayerNotNull(team.getPlayerOne())) {
            teamResult.setPlayerOneName(ServiceUtil.getPlayerOneNameBy(team));
        }
        if (ServiceUtil.availablePlayerNotNull(team.getPlayerTwo())) {
            teamResult.setPlayerTwoName(ServiceUtil.getPlayerTwoNameBy(team));
        }
//        setExpenseIfShareLose(startedGame.getGtype(), team.isWin(), totalBallCost, teamResult);
        return teamResult;
    }

    //    private TeamResult buildTeamTwoResult(CourtDTO court, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
//        List<CourtAreaDTO> teamOneList = court.getCourtAreas().stream().filter(area -> GameState.Player.PLAYER_A.equals(area.getArea()) || GameState.Player.PLAYER_B.equals(area.getArea()))
//                .collect(Collectors.toList());
//        return buildTeamResult(teamOneList, shuttleMap, startedGame);
//    }
//
//    private TeamResult buildTeamOneResult(CourtDTO court, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
//        List<CourtAreaDTO> teamTwoList = court.getCourtAreas().stream().filter(area -> GameState.Player.PLAYER_C.equals(area.getArea()) || GameState.Player.PLAYER_D.equals(area.getArea()))
//                .collect(Collectors.toList());
//        return buildTeamResult(teamTwoList, shuttleMap, startedGame);
//    }
//
//    private TeamResult buildTeamResult(List<CourtAreaDTO> teamList, Map<ShuttleBallResult, Integer> shuttleMap, Game startedGame) {
//        TeamResult teamResult = new TeamResult();
//
//        Float totalBallCost = gameExpenseCalculator.getTotalBallCost(shuttleMap);
//        float remainBallCost = totalBallCost / GameConstant.BISECT;
//        for (CourtAreaDTO courtAreaDTO : teamList) {
//            switch (courtAreaDTO.getArea()) {
//                case GameState.Player.PLAYER_A, GameState.Player.PLAYER_C:
//                    teamResult.setPlayerOneName(ServiceUtil.getPlayerInArea(courtAreaDTO));
//                    setPlayerOneExpenseIfShareLose(startedGame.getGtype(), courtAreaDTO.isWin(), remainBallCost, teamResult);
//                    break;
//                case GameState.Player.PLAYER_B, GameState.Player.PLAYER_D:
//                    teamResult.setPlayerTwoName(ServiceUtil.getPlayerInArea(courtAreaDTO));
//                    setPlayerTwoExpenseIfShareLose(startedGame.getGtype(), courtAreaDTO.isWin(), remainBallCost, teamResult);
//                    break;
//                default:
//                    break;
//            }
//            teamResult.setWin(ServiceUtil.getGameStatus(courtAreaDTO.isWin()));
//        }
//
//        return teamResult;
//    }

    private String findWinAreaDTO(Map<String, CourtAreaDTO> areaCourtMap) throws BusinessException {
        Map.Entry<String, CourtAreaDTO> entry = areaCourtMap.entrySet().stream().filter(map -> map.getValue().isWin()).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.WINNER_NOT_FOUND, "No specific winner."));
        return entry.getKey();
    }

    private Team findLoseTeam(Game game, CourtAreaDTO winAreaDTO) {
        if (ServiceUtil.isTeamOne(winAreaDTO.getArea())) {
            return game.getTeamTwo();
        }
        return game.getTeamOne();
    }

    private void setSharedExpenseToLoseTeam(float totalBallCost, Team loseTeam) {
        float sharedExpense = (float) totalBallCost / 2;
        loseTeam.setExpenseOne(sharedExpense);
        loseTeam.setExpenseTwo(sharedExpense);
    }

    private void validateGameFinishField(GameDTO gameRequest) {
        Assert.notNull(gameRequest.getCourt(), "Court must not be null.");
        Assert.isTrue(StringUtils.isNotBlank(gameRequest.getCourt().getCourtId()), "Court id must not be empty.");

        Integer.valueOf(gameRequest.getCourt().getCourtId());

        Assert.isTrue(StringUtils.isNotBlank(gameRequest.getCourt().getCourtName()), "Court name must not be empty.");
        Assert.notEmpty(gameRequest.getCourt().getCourtAreas(), "Court area must not be empty.");
        Assert.notEmpty(gameRequest.getShuttleBalls(), "Shuttle ball must not be empty.");
        boolean isValidTeamPlayers = haveValidTeamPlayers(gameRequest.getCourt().getCourtAreas(), getTotalBallExpense(gameRequest.getShuttleBalls()));
        Assert.isTrue(isValidTeamPlayers, "Player or expense is invalid.");
    }

    private Float getTotalBallExpense(List<ShuttleBallDTO> shuttleBalls) {
        return shuttleBalls.stream().map(b -> b.getShuttleCost() * b.getBallQuantity()).reduce(0f, Float::sum);
    }

    private boolean haveValidTeamPlayers(List<CourtAreaDTO> courtAreas, float totalBallExpense) {
        boolean hasWinner = false;
        int numTeamOne = 0;
        int numTeamTwo = 0;
        float actualExpenseTotal = MoneyUtils.DEFAULT;
        for (CourtAreaDTO area : courtAreas) {
            if (validCourtArea(area)) {
                hasWinner = checkWinner(area.isWin(), hasWinner);
                numTeamOne += checkTeamOne(area);
                numTeamTwo += checkTeamTwo(area);
                actualExpenseTotal += area.getPlayerInArea().getExpense();
            }
        }

        return validateTeamPlayer(hasWinner, numTeamOne, numTeamTwo, totalBallExpense, actualExpenseTotal);
    }

    private boolean validateTeamPlayer(boolean hasWinner, int numTeamOne, int numTeamTwo, float totalBallExpense, float actualExpenseTotal) {
        return hasWinner && numTeamOne == numTeamTwo && totalBallExpense == actualExpenseTotal;
    }

    private int checkTeamTwo(CourtAreaDTO area) {
        if (ServiceUtil.isTeamTwo(area.getArea())) {
            return 1;
        }
        return 0;
    }

    private int checkTeamOne(CourtAreaDTO area) {
        if (ServiceUtil.isTeamOne(area.getArea())) {
            return 1;
        }
        return 0;
    }

    private boolean checkWinner(boolean win, boolean hasWinner) {
        if (!hasWinner && win) {
            return true;
        }
        return hasWinner;
    }

    private boolean validCourtArea(CourtAreaDTO area) {
        return area != null &&
                area.getPlayerInArea() != null && CommonUtil.isNotNullEmpty(area.getArea(), area.getPlayerInArea().getPlayerName());
    }

    /**
     * Converts the finish-game request into the internal {@link GameModel}
     * handled by {@link CoreGameService}.
     */
    private GameModel calculateGameExpensetoGameModel(GameDTO gameRequest) {
        GameModel model = new GameModel();
        CourtModel courtModel = toCourtModel(gameRequest.getCourt());
        model.setCourtModel(courtModel);
        model.setGameState(GameState.getGameState(gameRequest.getGameState()));
        model.setGameType(toGameType(gameRequest.getGameType()));
        model.setShuttleBallModelMap(toShuttleBallMap(gameRequest.getShuttleBalls()));

        // Calculate game type, team expenses and win flags from court model
        calculateGameTypeAndExpenses(model, courtModel);
        return model;
    }

    private void calculateGameTypeAndExpenses(GameModel model, CourtModel courtModel) {
        // Map areas to a map for easier lookup
        Map<String, com.badminton.model.game.CourtAreaModel> areaMap = new HashMap<>();
        putAreaIfPresent(areaMap, courtModel.getAreaA());
        putAreaIfPresent(areaMap, courtModel.getAreaB());
        putAreaIfPresent(areaMap, courtModel.getAreaC());
        putAreaIfPresent(areaMap, courtModel.getAreaD());

        // Determine game type (NEGO if any area has WIN result with expense)
        GameType gameType = GameType.SHARE;
        for (com.badminton.model.game.CourtAreaModel area : areaMap.values()) {
            if (GameResultEnum.WIN.equals(area.getGameResult()) && area.getExpense() > MoneyUtils.DEFAULT) {
                gameType = GameType.NEGO;
                break;
            }
        }
        model.setGameType(gameType);

        // Set team expenses and win flags based on areas
        for (Map.Entry<String, com.badminton.model.game.CourtAreaModel> entry : areaMap.entrySet()) {
            setTeamExpenseInModel(model, entry.getKey(), entry.getValue().getExpense(),
                    GameResultEnum.WIN.equals(entry.getValue().getGameResult()));
        }
    }

    private void putAreaIfPresent(Map<String, com.badminton.model.game.CourtAreaModel> areaMap, com.badminton.model.game.CourtAreaModel area) {
        if (area != null && area.getArea() != null) {
            areaMap.put(area.getArea().name(), area);
        }
    }

    private void setTeamExpenseInModel(GameModel model, String area, float expense, boolean win) {
        switch (area) {
            case GameState.Player.PLAYER_A -> {
                model.setTeamOneExpenseOne(expense);
                model.setTeamOneWin(win);
            }
            case GameState.Player.PLAYER_B -> {
                model.setTeamOneExpenseTwo(expense);
                model.setTeamOneWin(win);
            }
            case GameState.Player.PLAYER_C -> {
                model.setTeamTwoExpenseOne(expense);
                model.setTeamTwoWin(win);
            }
            case GameState.Player.PLAYER_D -> {
                model.setTeamTwoExpenseTwo(expense);
                model.setTeamTwoWin(win);
            }
        }
    }

    private CourtModel toCourtModel(CourtDTO court) {
        CourtModel model = new CourtModel();
        model.setCourtId(court.getCourtId());
        model.setName(court.getCourtName());
        if (court.getCourtAreas() != null) {
            for (CourtAreaDTO areaDTO : court.getCourtAreas()) {
                setAreaIntoModel(model, areaDTO);
            }
        }
        return model;
    }

    private void setAreaIntoModel(CourtModel courtModel, CourtAreaDTO areaDTO) {
        if (areaDTO == null || StringUtils.isBlank(areaDTO.getArea())) {
            return;
        }
        CourtAreaModel areaModel = toCourtAreaModel(areaDTO);
        if (areaModel == null) {
            return; // unknown area letter — was a no-op in the old switch
        }
        switch (areaModel.getArea()) {
            case A -> courtModel.setAreaA(areaModel);
            case B -> courtModel.setAreaB(areaModel);
            case C -> courtModel.setAreaC(areaModel);
            case D -> courtModel.setAreaD(areaModel);
        }
    }

    private CourtAreaModel toCourtAreaModel(CourtAreaDTO areaDTO) {
        AreaEnum area;
        try {
            area = AreaEnum.valueOf(areaDTO.getArea().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
        CourtAreaModel model = new CourtAreaModel();
        model.setArea(area);
        model.setGameResult(areaDTO.isWin() ? GameResultEnum.WIN : GameResultEnum.LOOSE);
        if (areaDTO.getPlayerInArea() != null) {
            model.setAvailablePlayer(areaDTO.getPlayerInArea().getPlayerName());
            model.setExpense(areaDTO.getPlayerInArea().getExpense());
        }
        return model;
    }

    private GameType toGameType(String gameType) {
        String name = gameType == null ? null : GameType.getGameTypeString(gameType);
        return StringUtils.isBlank(name) ? null : GameType.valueOf(name);
    }

    private Map<ShuttleBallModel, Integer> toShuttleBallMap(List<ShuttleBallDTO> shuttleBalls) {
        Map<ShuttleBallModel, Integer> map = new HashMap<>();
        if (shuttleBalls == null) {
            return map;
        }
        for (ShuttleBallDTO ball : shuttleBalls) {
            if (ball == null) {
                continue;
            }
            ShuttleBallModel model = new ShuttleBallModel();
            model.setName(ball.getShuttleName());
            model.setCost(BigDecimal.valueOf(ball.getShuttleCost()));
            map.put(model, ball.getBallQuantity());
        }
        return map;
    }

    private void validateChangeGameStateRequest(GameDTO request) {
        Assert.notNull(request.getCourt(), "Court must not be null.");
        Assert.isTrue(StringUtils.isNotBlank(request.getCourt().getCourtId()), "Court id must not be empty.");
        Assert.notNull(GameState.getGameState(request.getGameState()),
                "Invalid game state: " + request.getGameState());
    }

    private boolean isRentGameType(String gameType) {
        return gameType != null && GameType.RENT.name().equals(GameType.getGameTypeString(gameType));
    }

    public boolean processStartGame(Game game, GameDTO gameRequest) throws BusinessException {
        requireTransition(game, GameState.START);
        if (gameRequest.getGameType() == null && !readyToStart(game.getTeamOne(), game.getTeamTwo())) {
            throw new BusinessException(ErrorCodeEnum.FLOW_ERROR, "Teams are not ready to start the game.");
        }
        if (gameRequest.getShuttleBalls() == null || gameRequest.getShuttleBalls().isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.SHUTTLE_BALL_NOT_FOUND,
                    "Shuttle ball must be selected to start the game.");
        }
        game.setState(GameState.START.getValue());

        game.setShuttleMap(createNewGameBallMap(game, gameRequest.getShuttleBalls()));
        setGameTypeIfPresent(game, gameRequest.getGameType());
        gameRepository.save(game);
        // reduce the shuttle ball quantity in stock
//        coreInventoryService.consumeShuttleBallsForGame(game);
        Result<Boolean> result = inventoryService.recordBallConsumption(toBallInGameRequest(gameRequest.getShuttleBalls(), game.getGameId()));
        if (!result.isSuccess()) {
            log.error("Cannot start the game due to record ball consumption failed");
            throw new RuntimeException("Cannot start the game due to record ball consumption failed");
        }
        return true;

    }

    private boolean processStartRentGame(Game game, GameDTO gameRequest) throws BusinessException {
        requireTransition(game, GameState.START);
        game.setState(GameState.START.getValue());
        game.setGtype(GameType.RENT.name());
        createNewGameBallMap(game, gameRequest.getShuttleBalls());
        gameRepository.save(game);
        // reduce the shuttle ball quantity in stock
        inventoryService.recordBallConsumption(toBallInGameRequest(game));
        return true;
    }


    private void requireTransition(Game game, GameState targetState) throws BusinessException {
        GameState currentState = game.getState() == null ? null : GameState.getGameState(game.getState());
        if (currentState == null || !ServiceUtil.validGameStateUpdate(currentState, targetState)) {
            throw new BusinessException(ErrorCodeEnum.FLOW_ERROR, String.format(
                    "Invalid game state transition: [%s] -> [%s].", game.getState(), targetState.getValue()));
        }
    }

    private boolean readyToStart(Team teamOne, Team teamTwo) {
        boolean teamOneReady = teamOne != null && (teamOne.getPlayerOne() != null || teamOne.getPlayerTwo() != null);
        boolean teamTwoReady = teamTwo != null && (teamTwo.getPlayerOne() != null || teamTwo.getPlayerTwo() != null);
        return teamOneReady && teamTwoReady;
    }

    private List<GameShuttleMap> createNewGameBallMap(Game game, List<ShuttleBallDTO> shuttleBalls) {
        List<GameShuttleMap> gameShuttleMaps = game.getShuttleMap() == null ? new ArrayList<>() : game.getShuttleMap();
        shuttleBalls.forEach(ball -> {
            gameShuttleMaps.add(shuttleBallService.createGameShuttleMap(
                    game, ball, ball.getBallQuantity()
            ));
        });
        return gameShuttleMaps;
    }

    private ShuttleBall convertDtoToEntity(ShuttleBallDTO selectedBall) {
        return new ShuttleBall(selectedBall.getShuttleName(), selectedBall.getShuttleCost());
    }

    private void setGameTypeIfPresent(Game game, String gameType) {
        if (gameType != null) {
            game.setGtype(GameType.getGameTypeString(gameType));
        }
    }

    private BallConsumeInGameRequest toBallInGameRequest(List<ShuttleBallDTO> shuttleBalls, int gameId) {
        BallConsumeInGameRequest consumeRequest = new BallConsumeInGameRequest();
        consumeRequest.setInGame(gameId);
        consumeRequest.setShuttleBallDTOList(shuttleBalls == null ? new ArrayList<>() : shuttleBalls);
        return consumeRequest;
    }

    private BallConsumeInGameRequest toBallInGameRequest(Game game) {
        BallConsumeInGameRequest request = new BallConsumeInGameRequest();
        request.setInGame(game.getGameId());
        List<ShuttleBallDTO> shuttleBallDTOList = new ArrayList<>();
        if (game.getShuttleMap() != null) {
            for (com.badminton.entity.GameShuttleMap mapping : game.getShuttleMap()) {
                if (mapping != null && mapping.getShuttleBall() != null) {
                    ShuttleBallDTO dto = new ShuttleBallDTO();
                    dto.setShuttleName(mapping.getShuttleBall().getShuttleName());
                    dto.setShuttleCost(mapping.getShuttleBall().getCost());
                    dto.setBallQuantity(mapping.getShuttleNumber());
                    shuttleBallDTOList.add(dto);
                }
            }
        }
        request.setShuttleBallDTOList(shuttleBallDTOList);
        return request;
    }

    private GameEndConfirmDTO toGameEndConfirmDTO(String courtId, boolean isFinishGame) {
        GameEndConfirmDTO dto = new GameEndConfirmDTO();
        dto.setCourtId(courtId);
        dto.setFinish(isFinishGame);
        return dto;
    }

    private GameModel toGameModelForTerminate(GameDTO gameRequest) {
        GameModel model = new GameModel();
        model.setCourtModel(toCourtModel(gameRequest.getCourt()));
        return model;
    }

}
