package com.badminton.exception.validation;

import com.badminton.constant.GameState;
import com.badminton.entity.Court;
import com.badminton.entity.Game;
import com.badminton.entity.ShuttleBall;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ShuttleBallDTO;

import java.util.List;
import java.util.Optional;

/**
 * Validations of the seating/removal flow. Each rule throws instead of returning a flag, so the
 * core services stay linear and the caller decides how the failure surfaces (log in
 * {@code CoreCourtService}, HTTP status in the controller).
 */
public class GameCheck {

    /**
     * A seat exists only for the four court areas.
     */
    public static void isCourtAreaValid(String area) throws BusinessException {
        if (!GameState.Player.PLAYER_A.equals(area) && !GameState.Player.PLAYER_B.equals(area)
                && !GameState.Player.PLAYER_C.equals(area) && !GameState.Player.PLAYER_D.equals(area)) {
            throw new BusinessException(ErrorCodeEnum.INCORRECT_TYPE,
                    "Court area [" + area + "] is not one of A, B, C, D.");
        }
    }

    /**
     * A court can hold a stale open row (state changed, ended_date left null). Seating next to it
     * would create a second active game, so the row has to be surfaced instead.
     *
     * @param openGames the court's games with {@code ended_date IS NULL}; an empty list is fine
     */
    public static void isGameNotStart(List<Game> openGames) throws BusinessException {
        boolean hasWaitingGame = openGames.stream()
                .anyMatch(game -> GameState.NOT_START.getValue().equals(game.getState()));
        if (hasWaitingGame || openGames.isEmpty()) {
            return;
        }
        Game staleGame = openGames.getFirst();
        throw new BusinessException(ErrorCodeEnum.GAME_STATE_ERROR,
                "Game " + staleGame.getGameId() + " of court " + staleGame.getCourt().getCourtName() + " is ["
                        + staleGame.getState() + "], expected [" + GameState.NOT_START.getValue() + "].");
    }

    /**
     * A game cannot be opened without the ball it is played with.
     */
    public static ShuttleBall isShuttleBallPresent(Optional<ShuttleBall> optBall, ShuttleBallDTO shuttleBallDTO)
            throws BusinessException {
        if (optBall == null || optBall.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.SHUTTLE_BALL_NOT_FOUND,
                    "No shuttle ball [" + shuttleBallDTO.getShuttleName() + "] with cost ["
                            + shuttleBallDTO.getShuttleCost() + "] in the catalog.");
        }
        return optBall.get();
    }

    /**
     * Nothing can be removed from a court that has no game waiting for players.
     */
    public static void isWaitingGamePresent(List<Game> waitingGames, Court court) throws BusinessException {
        if (waitingGames == null || waitingGames.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.GAME_NOT_FOUND,
                    "Court " + court.getCourtName() + " has no game in [" + GameState.NOT_START.getValue() + "].");
        }
    }

    /**
     * Removing a player has to release one; an already empty area is a broken request.
     */
    public static void isPlayerReleased(boolean released, Court court, String area) throws BusinessException {
        if (!released) {
            throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND,
                    "No player sits in area [" + area + "] of court " + court.getCourtName() + ".");
        }
    }
}
