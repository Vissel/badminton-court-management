package com.badminton.util;

import com.badminton.constant.GameConstant;
import com.badminton.entity.Court;
import com.badminton.entity.Game;
import com.badminton.entity.Session;
import com.badminton.entity.Team;
import com.badminton.response.result.CourtResult;
import com.badminton.response.result.GameResult;
import com.badminton.response.result.SessionResult;
import com.badminton.response.result.TeamResult;

public class Converter {
    public static SessionResult convertorToSessionResult(Session session) {
        SessionResult sessionResult = new SessionResult();
        sessionResult.setId(sessionResult.getId());
        sessionResult.setFromTime(session.getFromTime());
        sessionResult.setToTime(session.getToTime());
        return sessionResult;
    }

    public static GameResult convertorToGameResult(Game game) {
        GameResult gameResult = new GameResult();
        gameResult.setGameId(game.getGameId());
        gameResult.setCourtResult(convertorToCourtResult(game.getCourt()));
//        gameResult.setBallResultMap();
        gameResult.setTeamOneResult(convertorToTeamResult(game.getTeamOne()));
        gameResult.setTeamTwoResult(convertorToTeamResult(game.getTeamTwo()));
        gameResult.setState(game.getState());
        gameResult.setCreatedDate(game.getCreatedDate());
        gameResult.setEndedDate(game.getEndedDate());
        return gameResult;
    }

    private static TeamResult convertorToTeamResult(Team team) {
        TeamResult teamResult = new TeamResult();
        if (team.getPlayerOne() != null && team.getPlayerOne().getPlayer() != null) {
            teamResult.setPlayerOneName(team.getPlayerOne().getPlayer().getPlayerName());
            teamResult.setExpenseTwo(team.getExpenseOne());
        }
        if (team.getPlayerTwo() != null && team.getPlayerTwo().getPlayer() != null) {
            teamResult.setPlayerTwoName(team.getPlayerTwo().getPlayer().getPlayerName());
            teamResult.setExpenseTwo(team.getExpenseTwo());
        }
        teamResult.setWin(team.isWin() ? GameConstant.WIN : GameConstant.LOSE);
        return teamResult;
    }

    private static CourtResult convertorToCourtResult(Court court) {
        return new CourtResult(court.getCourtId(), court.getCourtName());
    }
}
