package com.badminton.service.calculator;

import com.badminton.constant.GameConstant;
import com.badminton.entity.Game;
import com.badminton.response.result.CourtResult;
import com.badminton.response.result.GameResult;
import com.badminton.response.result.ShuttleBallResult;
import com.badminton.response.result.TeamResult;
import com.badminton.util.ServiceUtil;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameExpenseCalculator {

    public GameResult calculateGameResult(Game game) {
        GameResult gameResult = new GameResult();
        gameResult.setGameId(game.getGameId());
        gameResult.setCourtResult(new CourtResult(game.getCourt().getCourtId(), game.getCourt().getCourtName()));
        calculateTeamExpensesOfGameByBallUsed(gameResult, game);
        gameResult.setCreatedDate(gameResult.getCreatedDate());
        gameResult.setEndedDate(gameResult.getEndedDate());
        return gameResult;
    }

    private void calculateTeamExpensesOfGameByBallUsed(GameResult gameResult, Game game) {
        Map<ShuttleBallResult, Integer> ballResMap = game.getShuttleMap().stream().collect(Collectors.toMap(
                map -> new ShuttleBallResult(map.getShuttleBall().getShuttleName(), map.getShuttleBall().getCost()),
                map -> Integer.valueOf("1"),
                Integer::sum // merge function — sum counts if duplicates exist
        ));
        gameResult.setBallResultMap(ballResMap);
        // normal divided cost to both
        float totalBallCost = ballResMap.entrySet().stream().map(
                e -> (float) e.getKey().getCost() * e.getValue()
        ).reduce(0f, Float::sum);
        boolean teamOneWin = GameConstant.WIN.equals(game.getState());

        TeamResult teamOneRes = new TeamResult();
        teamOneRes.setPlayerOneName(ServiceUtil.getPlayerOneNameBy(game.getTeamOne()));
        teamOneRes.setPlayerTwoName(ServiceUtil.getPlayerOneNameBy(game.getTeamOne()));
        TeamResult teamTwoRes = new TeamResult();
        teamTwoRes.setPlayerOneName(ServiceUtil.getPlayerOneNameBy(game.getTeamTwo()));
        teamTwoRes.setPlayerTwoName(ServiceUtil.getPlayerOneNameBy(game.getTeamTwo()));
        if (teamOneWin) {
            dividedExpenseTo(teamTwoRes, totalBallCost, 2f);
            teamTwoRes.setWin(GameConstant.LOSE);
        } else {
            dividedExpenseTo(teamOneRes, totalBallCost, 2f);
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
