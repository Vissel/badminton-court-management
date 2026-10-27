package com.badminton.core.game;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.core.shuttleball.CoreShuttleBallService;
import com.badminton.dao.DaoGame;
import com.badminton.entity.*;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.exception.validation.AvailablePlayerCheck;
import com.badminton.exception.validation.GameCheck;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.model.game.*;
import com.badminton.repository.GameRepository;
import com.badminton.repository.TeamRepository;
import com.badminton.util.MoneyUtils;
import com.badminton.util.ServiceUtil;
import com.badminton.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class CoreGameService {

    @Autowired
    GameRepository gameRepository;
    @Autowired
    TeamRepository teamRepository;
    @Autowired
    DaoGame daoGame;
    @Autowired
    CoreShuttleBallService coreShuttleBallService;
    @Autowired
    CoreAvailablePlayerService coreAvailablePlayerService;

    /**
     * Finish the active game on the court described by {@link GameModel}:
     * uses pre-calculated game type, team expenses and win flags from GameModel,
     * stamps endedDate and persists the game as FINISH.
     */
    @Transactional
    public Boolean finishGame(GameModel gameModel) throws BusinessException {
        Game game = daoGame.findActiveGameByCourtIdForUpdate(gameModel.getCourtModel().getCourtId())
                .orElseThrow(() -> new BusinessException(ErrorCodeEnum.GAME_NOT_FOUND, "Game is not found to execute."));
        game.setGtype(gameModel.getGameType().name());
        // Set team expenses and win flags from GameModel
        if (gameModel.getTeamOneExpenseOne() != null) {
            game.getTeamOne().setExpenseOne(gameModel.getTeamOneExpenseOne());
        }
        if (gameModel.getTeamOneExpenseTwo() != null) {
            game.getTeamOne().setExpenseTwo(gameModel.getTeamOneExpenseTwo());
        }
        if (gameModel.getTeamOneWin() != null) {
            game.getTeamOne().setWin(gameModel.getTeamOneWin());
        }
        if (gameModel.getTeamTwoExpenseOne() != null) {
            game.getTeamTwo().setExpenseOne(gameModel.getTeamTwoExpenseOne());
        }
        if (gameModel.getTeamTwoExpenseTwo() != null) {
            game.getTeamTwo().setExpenseTwo(gameModel.getTeamTwoExpenseTwo());
        }
        if (gameModel.getTeamTwoWin() != null) {
            game.getTeamTwo().setWin(gameModel.getTeamTwoWin());
        }
        // Record the game cost as a service onto each player's available-player
        // row so the SHARE/NEGO expense is billed. Players with zero expense are
        // skipped inside buildService().
        recordCostServiceToPlayers(game, gameModel.getCourtModel());

        // set value: state, endedDate.
        game.setEndedDate(TimeUtils.getUTCPlus7Instant());
        game.setState(GameState.FINISH.getValue());
        gameRepository.save(game);
        return true;
    }

    public Game findActiveGame(String courtId) throws BusinessException {
        Optional<Game> gOption = daoGame.findActiveGameByCourtId(courtId);
        if (!gOption.isPresent()) {
            throw new BusinessException(ErrorCodeEnum.GAME_NOT_FOUND, "Game is not found to execute.");
        }
        return gOption.get();
    }


    public GameModel findActiveGameModel(int gameId) throws BusinessException {
        Optional<Game> gOption = gameRepository.findById(gameId);
        if (!gOption.isPresent()) {
            throw new BusinessException(ErrorCodeEnum.GAME_NOT_FOUND, "Game is not found to execute.");
        }
        return convertToGameModel(gOption.get());
    }

    public int findActiveGameId(String courtId) throws BusinessException {
        Game game = findActiveGame(courtId);
        return game.getGameId();
    }

    private GameModel convertToGameModel(Game game) {
        GameModel model = new GameModel();
        // Convert court to CourtModel
        CourtModel courtModel = new CourtModel();
        courtModel.setCourtId(String.valueOf(game.getCourt().getCourtId()));
        courtModel.setName(game.getCourt().getCourtName());
        model.setCourtModel(courtModel);

        // Convert state to GameState
        model.setGameState(GameState.getGameState(game.getState()));

        // Convert gtype to GameType
        if (game.getGtype() != null) {
            model.setGameType(GameType.valueOf(game.getGtype()));
        }

        // Convert shuttleMap to Map<ShuttleBallModel, Integer>
        if (game.getShuttleMap() != null) {
            Map<ShuttleBallModel, Integer> shuttleBallMap = new HashMap<>();
            for (GameShuttleMap map : game.getShuttleMap()) {
                ShuttleBallModel shuttleBallModel = new ShuttleBallModel();
                shuttleBallModel.setName(map.getShuttleBall().getShuttleName());
                shuttleBallModel.setCost(java.math.BigDecimal.valueOf(map.getShuttleBall().getCost()));
                shuttleBallMap.put(shuttleBallModel, map.getShuttleNumber());
            }
            model.setShuttleBallModelMap(shuttleBallMap);
        }

        // Set team expenses and win flags from game teams
        if (game.getTeamOne() != null) {
            model.setTeamOneExpenseOne(game.getTeamOne().getExpenseOne());
            model.setTeamOneExpenseTwo(game.getTeamOne().getExpenseTwo());
            model.setTeamOneWin(game.getTeamOne().isWin());
        }
        if (game.getTeamTwo() != null) {
            model.setTeamTwoExpenseOne(game.getTeamTwo().getExpenseOne());
            model.setTeamTwoExpenseTwo(game.getTeamTwo().getExpenseTwo());
            model.setTeamTwoWin(game.getTeamTwo().isWin());
        }

        return model;
    }

    private void findGTypeAndSetExpense(Game game, CourtModel courtModel) {
        Map<String, CourtAreaModel> areaMap = mapAreaStrKey(courtModel);
        GameType gType = findGameType(areaMap);
        // set gType to game
        game.setGtype(gType.name());
        // set expense
        setTeamExpenseFromAreaMap(game, areaMap);
    }

    private Map<String, CourtAreaModel> mapAreaStrKey(CourtModel courtModel) {
        Map<String, CourtAreaModel> areaMap = new HashMap<>();
        putAreaIfPresent(areaMap, courtModel.getAreaA());
        putAreaIfPresent(areaMap, courtModel.getAreaB());
        putAreaIfPresent(areaMap, courtModel.getAreaC());
        putAreaIfPresent(areaMap, courtModel.getAreaD());
        return areaMap;
    }

    private void putAreaIfPresent(Map<String, CourtAreaModel> areaMap, CourtAreaModel area) {
        if (area != null && area.getArea() != null) {
            areaMap.put(area.getArea().name(), area);
        }
    }

    private GameType findGameType(Map<String, CourtAreaModel> areaMap) {
        for (CourtAreaModel area : areaMap.values()) {
            if (GameResultEnum.WIN.equals(area.getGameResult()) && area.getExpense() != MoneyUtils.DEFAULT) {
                return GameType.NEGO;
            }
        }
        return GameType.SHARE;
    }

    private void setTeamExpenseFromAreaMap(Game game, Map<String, CourtAreaModel> areaMap) {
        for (Map.Entry<String, CourtAreaModel> entry : areaMap.entrySet()) {
            setTeamExpense(game, entry.getKey(), entry.getValue().getExpense(),
                    GameResultEnum.WIN.equals(entry.getValue().getGameResult()));
        }
    }

    private void setTeamExpense(Game game, String area, float expense, boolean win) {
        switch (area) {
            case GameState.Player.PLAYER_A -> {
                game.getTeamOne().setExpenseOne(expense);
                game.getTeamOne().setWin(win);
                setServiceInToAvaPlayer(game.getTeamOne().getPlayerOne(), buildService(game.getCourt().getCourtName(), expense));
            }
            case GameState.Player.PLAYER_B -> {
                game.getTeamOne().setExpenseTwo(expense);
                game.getTeamOne().setWin(win);
                setServiceInToAvaPlayer(game.getTeamOne().getPlayerTwo(), buildService(game.getCourt().getCourtName(), expense));
            }
            case GameState.Player.PLAYER_C -> {
                game.getTeamTwo().setExpenseOne(expense);
                game.getTeamTwo().setWin(win);
                setServiceInToAvaPlayer(game.getTeamTwo().getPlayerOne(), buildService(game.getCourt().getCourtName(), expense));
            }
            case GameState.Player.PLAYER_D -> {
                game.getTeamTwo().setExpenseTwo(expense);
                game.getTeamTwo().setWin(win);
                setServiceInToAvaPlayer(game.getTeamTwo().getPlayerTwo(), buildService(game.getCourt().getCourtName(), expense));
            }
        }
    }

    private void setServiceInToAvaPlayer(AvailablePlayer player, ServiceDTO addedService) {
        if (addedService == null) return;
        player.setServices(ServiceUtil.addServiceToJsonArray(player.getCurrentServices(), addedService));
    }

    private ServiceDTO buildService(String courtName, float expense) {
        if (expense > MoneyUtils.DEFAULT) {
            ServiceDTO expenseSer = new ServiceDTO();
            expenseSer.setServiceName("Tiền ".concat(courtName));
            expenseSer.setCost(expense);
            return expenseSer;
        }
        return null;
    }

    @Transactional
    public Boolean cancelGame(GameModel gameModel) throws BusinessException {
        Game game = daoGame.findActiveGameByCourtIdForUpdate(gameModel.getCourtModel().getCourtId()).get();
        game.setEndedDate(TimeUtils.getUTCPlus7Instant());
        game.setState(GameState.CANCEL.getValue());
        gameRepository.save(game);
        return true;
    }

    /**
     * Close the active RENT game of the court: stamps endedDate and moves it to
     * the requested terminal state. Used by the rent-by-time business so it never
     * writes the Game row itself.
     *
     * <p>Idempotent: when the court has no open game, or its open game is not a rent
     * game, this operation owns nothing, so it reports success and only logs. A
     * rental that was already paid must not fail because its game row was closed
     * elsewhere.</p>
     */
    @Transactional
    public Boolean endRentGame(String courtId, GameState targetState) {
        if (!ServiceUtil.isEndedState(targetState)) {
            throw new IllegalArgumentException("Only Finish or Cancel can end a rent game.");
        }
        Optional<Game> activeGame = daoGame.findActiveGameByCourtIdForUpdate(courtId);
        if (activeGame.isEmpty()) {
            log.info("No open game on court {}: rent game is already closed.", courtId);
            return Boolean.TRUE;
        }
        Game game = activeGame.get();
        if (!GameType.RENT.name().equals(game.getGtype())) {
            log.warn("Open game {} on court {} is {} instead of a rent game, leaving it untouched.",
                    game.getGameId(), courtId, game.getGtype());
            return Boolean.TRUE;
        }
        game.setState(targetState.getValue());
        game.setEndedDate(TimeUtils.getUTCPlus7Instant());
        gameRepository.save(game);
        return Boolean.TRUE;
    }

    /**
     * Compensating action for {@link #cancelGame}: restore a game that was just
     * cancelled back to its prior active state (clear endedDate + previous state).
     * Used when a downstream step (e.g. inventory confirmation) fails after the
     * cancel has already been committed.
     */
    @Transactional
    public void revertCancelledGame(int gameId, String previousState) {
        gameRepository.findById(gameId).ifPresent(game -> {
            game.setEndedDate(null);
            game.setState(previousState);
            gameRepository.save(game);
        });
    }

    /**
     * Capture the mutable fields of the active game on a court before a finish
     * transition, so {@link #revertFinishedGame} can undo it as a compensating
     * action. Runs in a read transaction so lazy team associations load safely.
     */
    @Transactional(readOnly = true)
    public GameSnapshot snapshotActiveGame(String courtId) throws BusinessException {
        Game game = findActiveGame(courtId);
        GameSnapshot snapshot = new GameSnapshot();
        snapshot.setGameId(game.getGameId());
        snapshot.setState(game.getState());
        snapshot.setEndedDate(game.getEndedDate());
        snapshot.setGtype(game.getGtype());
        if (game.getTeamOne() != null) {
            snapshot.setTeamOneExpenseOne(game.getTeamOne().getExpenseOne());
            snapshot.setTeamOneExpenseTwo(game.getTeamOne().getExpenseTwo());
            snapshot.setTeamOneWin(game.getTeamOne().isWin());
        }
        if (game.getTeamTwo() != null) {
            snapshot.setTeamTwoExpenseOne(game.getTeamTwo().getExpenseOne());
            snapshot.setTeamTwoExpenseTwo(game.getTeamTwo().getExpenseTwo());
            snapshot.setTeamTwoWin(game.getTeamTwo().isWin());
        }
        return snapshot;
    }

    /**
     * Compensating action for {@link #finishGame}: restore the game to the state
     * captured by {@link #snapshotActiveGame} (state, endedDate, gtype and team
     * expenses/win flags). Used when a downstream step (e.g. inventory
     * confirmation) fails after the finish has already been committed.
     */
    @Transactional
    public void revertFinishedGame(GameSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        gameRepository.findById(snapshot.getGameId()).ifPresent(game -> {
            game.setState(snapshot.getState());
            game.setEndedDate(snapshot.getEndedDate());
            game.setGtype(snapshot.getGtype());
            if (game.getTeamOne() != null) {
                if (snapshot.getTeamOneExpenseOne() != null) {
                    game.getTeamOne().setExpenseOne(snapshot.getTeamOneExpenseOne());
                }
                if (snapshot.getTeamOneExpenseTwo() != null) {
                    game.getTeamOne().setExpenseTwo(snapshot.getTeamOneExpenseTwo());
                }
                if (snapshot.getTeamOneWin() != null) {
                    game.getTeamOne().setWin(snapshot.getTeamOneWin());
                }
            }
            if (game.getTeamTwo() != null) {
                if (snapshot.getTeamTwoExpenseOne() != null) {
                    game.getTeamTwo().setExpenseOne(snapshot.getTeamTwoExpenseOne());
                }
                if (snapshot.getTeamTwoExpenseTwo() != null) {
                    game.getTeamTwo().setExpenseTwo(snapshot.getTeamTwoExpenseTwo());
                }
                if (snapshot.getTeamTwoWin() != null) {
                    game.getTeamTwo().setWin(snapshot.getTeamTwoWin());
                }
            }
            gameRepository.save(game);
        });
    }

    public Boolean checkGameExistById(int gameId) {
        return gameRepository.findById(gameId).isPresent();
    }

    /**
     * A court is rentable while it still holds an open game that has not been started.
     * Read through the derived finder (court + state + endedDate is null) - no hand-written
     * JPQL is needed for it.
     */
    public Boolean isGameNotStartInCourt(Court court) {
        return gameRepository.findByCourtAndStateAndEndedDateIsNull(court, GameState.NOT_START.getValue())
                .isPresent();
    }

    /**
     * Seat a player in the requested area of the court's game, opening that game with the chosen
     * shuttle ball when the court has none yet. The game row and the teams hanging off it are
     * this service's; the ball and the player are asked from their own cores.
     *
     * @throws BusinessException when the court's open game is not in "Not start", the shuttle ball
     *                           is not in the catalog, or the player is not in the current session
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean addPlayerToCourt(Court court, String playerName, String area, ShuttleBallDTO shuttleBallDTO)
            throws BusinessException {
        GameCheck.isCourtAreaValid(area);
        // Every open row of this court counts (endedDate is null), whatever its state: a stale
        // 'Start'/'Finish'/'Cancel' row must be surfaced instead of silently getting a second
        // active game next to it.
        List<Game> openGames = gameRepository.findAllByCourtIdAndEndedDateIsNull(court.getCourtId());
        GameCheck.isGameNotStart(openGames);
        // orElseGet cannot carry the checked failure of opening a game, so the fallback stays explicit.
        Optional<Game> waitingGame = notStartGame(openGames);
        Game game = waitingGame.isPresent() ? waitingGame.get() : openGame(court, shuttleBallDTO);
        return seatPlayer(game, playerName, area);
    }

    private Optional<Game> notStartGame(List<Game> openGames) {
        return openGames.stream()
                .filter(game -> GameState.NOT_START.getValue().equals(game.getState()))
                .findFirst();
    }

    /**
     * Open the court's game with the chosen shuttle ball.
     */
    private Game openGame(Court court, ShuttleBallDTO shuttleBallDTO) throws BusinessException {
        ShuttleBall ball = GameCheck.isShuttleBallPresent(coreShuttleBallService
                .findActiveBall(shuttleBallDTO.getShuttleName(), shuttleBallDTO.getShuttleCost()), shuttleBallDTO);
        return gameRepository.save(new Game(court, ball));
    }

    /**
     * Put the player in the slot the area names. A new team is attached to the game and persisted
     * with it (the game owns the team row); an existing team is managed, so the assignment is
     * flushed with the game as well.
     */
    private boolean seatPlayer(Game game, String playerName, String area) throws BusinessException {
        Optional<AvailablePlayer> playerOpt = coreAvailablePlayerService.findAvailableByNameInSession(playerName);
        AvailablePlayerCheck.isAvailablePlayerPresent(playerOpt);
        AvailablePlayer player = playerOpt.get();
        Team team = teamOf(game, area);
        if (isFirstSlot(area)) {
            team.setPlayerOne(player);
        } else {
            team.setPlayerTwo(player);
        }
        if (team.getTeamId() == 0) {
            if (isTeamOneArea(area)) {
                game.setTeamOne(team);
            } else {
                game.setTeamTwo(team);
            }
        }
        gameRepository.save(game);
        log.info("Added {} to area {} of court {} (game {}).", playerName, area,
                game.getCourt().getCourtName(), game.getGameId());
        return true;
    }

    /**
     * A and C are the first player of a team, B and D the second; the area itself is already
     * validated by {@link GameCheck#isCourtAreaValid(String)}.
     */
    private boolean isFirstSlot(String area) {
        return GameState.Player.PLAYER_A.equals(area) || GameState.Player.PLAYER_C.equals(area);
    }

    private Team teamOf(Game game, String area) {
        Team team = isTeamOneArea(area) ? game.getTeamOne() : game.getTeamTwo();
        if (team != null) {
            return team;
        }
        Team newTeam = new Team();
        newTeam.setGame(game);
        return newTeam;
    }

    /**
     * Areas A and B belong to the first team, C and D to the second; an area outside that set is
     * rejected by {@link GameCheck#isCourtAreaValid(String)}, so it can only be the first team.
     */
    private boolean isTeamOneArea(String area) {
        return !GameState.Player.PLAYER_C.equals(area) && !GameState.Player.PLAYER_D.equals(area);
    }

    /**
     * Free the requested area of the games waiting on the court and drop every waiting game that no
     * longer holds a player: an empty court keeps no game row. Games are selected by state, so a row
     * whose state went stale cannot hide the waiting one.
     *
     * @throws BusinessException when the court has no game in "Not start", or none of them holds a
     *                           player in that area
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean removePlayerFromCourt(Court court, String area) throws BusinessException {
        GameCheck.isCourtAreaValid(area);
        List<Game> waitingGames = waitingGames(court);
        if (waitingGames.isEmpty()) {
            log.warn("Court {} has no game in [{}], nothing to remove. Open game states on the court: {}",
                    court.getCourtName(), GameState.NOT_START.getValue(),
                    gameRepository.findAllByCourtIdAndEndedDateIsNull(court.getCourtId()).stream()
                            .map(Game::getState).toList());
        }
        GameCheck.isWaitingGamePresent(waitingGames, court);

        boolean released = false;
        for (Game game : waitingGames) {
            released |= freeSeat(game, area);
        }
        GameCheck.isPlayerReleased(released, court, area);

        waitingGames.stream().filter(this::isCourtEmpty).forEach(this::deleteGame);
        return true;
    }

    /**
     * The court's games that are still waiting for players.
     */
    private List<Game> waitingGames(Court court) {
        List<Game> courtGames = gameRepository
                .findAllByStateInAndEndedDateIsNull(Set.of(GameState.NOT_START.getValue())).stream()
                .filter(game -> game.getCourt() != null && game.getCourt().getCourtId() == court.getCourtId())
                .toList();
        if (courtGames.size() > 1) {
            log.warn("Court {} has {} games in [{}]: {}", court.getCourtName(), courtGames.size(),
                    GameState.NOT_START.getValue(), courtGames.stream().map(Game::getGameId).toList());
        }
        return courtGames;
    }

    /**
     * Remove a waiting game together with its teams and shuttle map. The game's own team links are
     * dropped first: {@code game.team_id1/team_id2} cascade ON DELETE towards {@code team}, so
     * deleting a team while the game still points at it wipes the game row behind Hibernate's back
     * and makes its own delete fail.
     */
    private void deleteGame(Game game) {
        final int gameId = game.getGameId();
        List<Team> teams = new ArrayList<>();
        if (game.getTeamOne() != null) {
            teams.add(game.getTeamOne());
        }
        if (game.getTeamTwo() != null && !teams.contains(game.getTeamTwo())) {
            teams.add(game.getTeamTwo());
        }

        game.setTeamOne(null);
        game.setTeamTwo(null);
        gameRepository.saveAndFlush(game);
        teamRepository.deleteAll(teams);
        teamRepository.flush();

        gameRepository.delete(game);
        gameRepository.flush();
        log.info("Game {} removed from court {} with {} team(s).", gameId, game.getCourt().getCourtName(),
                teams.size());
    }

    /**
     * Clear the seat the area names, leaving every other slot untouched.
     *
     * @return false when the game carries no team on that side or the seat is already empty; the
     *         caller decides whether any game of the court released the player
     */
    private boolean freeSeat(Game game, String area) {
        final boolean firstSlot = isFirstSlot(area);
        Team team = isTeamOneArea(area) ? game.getTeamOne() : game.getTeamTwo();
        if (team == null) {
            return false;
        }
        AvailablePlayer player = firstSlot ? team.getPlayerOne() : team.getPlayerTwo();
        if (player == null) {
            return false;
        }
        if (firstSlot) {
            team.setPlayerOne(null);
        } else {
            team.setPlayerTwo(null);
        }
        log.info("Released {} from area {} of game {}.", player.getPlayer().getPlayerName(), area, game.getGameId());
        return true;
    }

    private boolean isCourtEmpty(Game game) {
        return !hasPlayer(game.getTeamOne()) && !hasPlayer(game.getTeamTwo());
    }

    private boolean hasPlayer(Team team) {
        return team != null && (team.getPlayerOne() != null || team.getPlayerTwo() != null);
    }

    /**
     * Append the "Tiền {courtName}" cost service to the available player sitting
     * in each occupied area, based on that area's expense in the court model.
     */
    private void recordCostServiceToPlayers(Game game, CourtModel courtModel) {
        Map<String, CourtAreaModel> areaMap = mapAreaStrKey(courtModel);
        for (Map.Entry<String, CourtAreaModel> entry : areaMap.entrySet()) {
            AvailablePlayer player = resolvePlayerByArea(game, entry.getKey());
            setServiceInToAvaPlayer(player,
                    buildService(game.getCourt().getCourtName(), entry.getValue().getExpense()));
        }
    }

    private AvailablePlayer resolvePlayerByArea(Game game, String area) {
        switch (area) {
            case GameState.Player.PLAYER_A -> {
                return game.getTeamOne() != null ? game.getTeamOne().getPlayerOne() : null;
            }
            case GameState.Player.PLAYER_B -> {
                return game.getTeamOne() != null ? game.getTeamOne().getPlayerTwo() : null;
            }
            case GameState.Player.PLAYER_C -> {
                return game.getTeamTwo() != null ? game.getTeamTwo().getPlayerOne() : null;
            }
            case GameState.Player.PLAYER_D -> {
                return game.getTeamTwo() != null ? game.getTeamTwo().getPlayerTwo() : null;
            }
            default -> {
                return null;
            }
        }
    }
}
