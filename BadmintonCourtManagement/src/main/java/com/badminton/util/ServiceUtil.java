package com.badminton.util;

import com.badminton.constant.CommonConstant;
import com.badminton.constant.GameConstant;
import com.badminton.constant.GameState;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.Team;
import com.badminton.requestmodel.CourtAreaDTO;
import com.badminton.response.result.ShuttleBallResult;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceUtil {

    public static String buildService(String serviceName, float cost) {
        return serviceName.concat(CommonConstant.HYPHEN).concat(String.valueOf(cost));
    }

    /**
     * Condition true: Not start -> Started <br>
     * Started -> Finish <br>
     * Started -> Cancel
     *
     * @param current
     * @param change
     * @return
     */
    public static boolean validGameStateUpdate(GameState current, GameState change) {
        return (current.equals(GameState.NOT_START) && change.equals(GameState.START))
                || (current.equals(GameState.START) && isEndedState(change));
    }

    public static boolean isEndedState(GameState change) {
        return (change.equals(GameState.FINISH) || change.equals(GameState.CANCEL));
    }

    public static String getNowTimeString() {
        // Get the current date and time
        LocalDateTime now = LocalDateTime.now();
        // Define the desired format for MySQL TIMESTAMP
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // Format the LocalDateTime object into a string
        return now.format(formatter);
    }

    public static Timestamp getCurrentTimeStamp() {
        return new Timestamp(System.currentTimeMillis());
    }

    public static String getPlayerOneNameBy(Team team) {
        return team.getPlayerOne().getPlayer().getPlayerName();
    }

    public static String getPlayerTwoNameBy(Team team) {
        return team.getPlayerTwo().getPlayer().getPlayerName();
    }

    public static String getPlayerInArea(CourtAreaDTO courtArea) {
        return courtArea.getPlayerInArea().getPlayerName();
    }

    public static Map<ShuttleBallResult, Integer> retrievedShuttleBallMap(List<GameShuttleMap> gameShuttleMapping) {
        return gameShuttleMapping.stream().collect(Collectors.toMap(
                map -> new ShuttleBallResult(map.getShuttleBall().getShuttleName(), map.getShuttleBall().getCost()),
                map -> Integer.valueOf(map.getShuttleNumber()),
                Integer::sum // merge function — sum counts if duplicates exist
        ));
    }

    public static String getGameStatus(boolean isWin) {
        return isWin ? GameConstant.WIN : GameConstant.LOSE;
    }

    public static boolean availablePlayerNotNull(AvailablePlayer availablePlayer) {
        return availablePlayer != null && availablePlayer.getPlayer() != null;
    }
}
