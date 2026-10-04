package com.badminton.core.game;

import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.game.CourtAreaModel;
import com.badminton.model.game.CourtModel;
import com.badminton.model.game.GameModel;
import com.badminton.model.game.GameResultEnum;
import com.badminton.repository.GameRepository;
import com.badminton.util.MoneyUtils;
import com.badminton.util.ServiceUtil;
import com.badminton.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CoreGameService {

    @Autowired
    GameRepository gameRepository;

    /**
     * Finish the active game on the court described by {@link GameModel}:
     * resolves gType (SHARE/NEGO), writes team expenses + win flags, bills a
     * "Tiền {courtName}" service per occupied area, stamps endedDate and
     * persists the game as FINISH.
     */
    public Boolean finishGame(GameModel gameModel) throws BusinessException {
        Game game = findActiveGame(gameModel.getCourtModel().getCourtId());
        // validate from request the type of game: SHARE or NEGO and set expense
        findGTypeAndSetExpense(game, gameModel.getCourtModel());
        // set value: state, Team's expense, endedDate, gType.
        game.setEndedDate(TimeUtils.getUTCPlus7Instant());
        game.setState(GameState.FINISH.getValue());
        gameRepository.save(game);

        return true;
    }

    public Game findActiveGame(String courtId) throws BusinessException {
        Optional<Game> gOption = gameRepository.findByCourtIdAndEndedDateIsNull(Integer.valueOf(courtId));
        if (!gOption.isPresent()) {
            throw new BusinessException(ErrorCodeEnum.GAME_NOT_FOUND, "Game is not found to execute.");
        }
        return gOption.get();
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

    public Boolean cancelGame(GameModel gameModel) {
        return Boolean.TRUE;
    }

    public Boolean checkGameExistById(int gameId) {
        return gameRepository.findById(gameId).isPresent();
    }
}
