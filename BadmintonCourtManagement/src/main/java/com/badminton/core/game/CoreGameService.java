package com.badminton.core.game;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.dao.DaoGame;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.game.*;
import com.badminton.repository.GameRepository;
import com.badminton.util.MoneyUtils;
import com.badminton.util.ServiceUtil;
import com.badminton.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CoreGameService {

    @Autowired
    GameRepository gameRepository;
    @Autowired
    DaoGame daoGame;

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
}
