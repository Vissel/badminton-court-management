package com.badminton.service.calculator;

import com.badminton.constant.GameConstant;
import com.badminton.entity.Game;
import com.badminton.response.result.CourtResult;
import com.badminton.response.result.GameResult;
import com.badminton.response.result.ShuttleBallResult;
import com.badminton.response.result.TeamResult;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class GameExpenseCalculator {
    /**
     * get total cost by shuttle ball in game
     *
     * @param ballResMap
     * @return
     */
    public Float getTotalBallCost(Map<ShuttleBallResult, Integer> ballResMap) {
        return ballResMap.entrySet().stream().map(
                e -> (float) e.getKey().getCost() * e.getValue()
        ).reduce(0f, Float::sum);
    }

    public GameResult calculateGameResult(Game game) {
        log.info("Calculate GameResult of GameId: {}", game.getGameId());
        GameResult gameResult = new GameResult();
        gameResult.setGameId(game.getGameId());
        gameResult.setCourtResult(new CourtResult(game.getCourt().getCourtId(), game.getCourt().getCourtName()));
        calculateTeamExpensesOfGameByBallUsed(gameResult, game);
        gameResult.setCreatedDate(gameResult.getCreatedDate());
        gameResult.setEndedDate(gameResult.getEndedDate());
        return gameResult;
    }

    private void calculateTeamExpensesOfGameByBallUsed(GameResult gameResult, Game game) {
        Map<ShuttleBallResult, Integer> ballResMap = ServiceUtil.retrievedShuttleBallMap(game.getShuttleMap());
        gameResult.setBallResultMap(ballResMap);
        // normal divided cost to both
        float totalBallCost = getTotalBallCost(ballResMap);
        boolean teamOneWin = GameConstant.WIN.equals(game.getState());

        TeamResult teamOneRes = new TeamResult();
        teamOneRes.setPlayerOneName(ServiceUtil.getPlayerOneNameBy(game.getTeamOne()));
        teamOneRes.setPlayerTwoName(ServiceUtil.getPlayerTwoNameBy(game.getTeamOne()));
        TeamResult teamTwoRes = new TeamResult();
        teamTwoRes.setPlayerOneName(ServiceUtil.getPlayerOneNameBy(game.getTeamTwo()));
        teamTwoRes.setPlayerTwoName(ServiceUtil.getPlayerTwoNameBy(game.getTeamTwo()));
        if (teamOneWin) {
            dividedExpenseTo(teamTwoRes, totalBallCost, GameConstant.BISECT);
            teamTwoRes.setWin(GameConstant.LOSE);
        } else {
            dividedExpenseTo(teamOneRes, totalBallCost, GameConstant.BISECT);
            teamOneRes.setWin(GameConstant.LOSE);
        }

        gameResult.setTeamOneResult(teamOneRes);
        gameResult.setTeamTwoResult(teamTwoRes);
    }

    private void dividedExpenseTo(TeamResult teamResult, float totalCost, float toFloat) {
        teamResult.setExpenseOne(totalCost / toFloat);
        teamResult.setExpenseTwo(totalCost / toFloat);
    }
}
