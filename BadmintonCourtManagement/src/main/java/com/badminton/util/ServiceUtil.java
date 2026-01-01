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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceUtil {

    /**
     * build service string by name and cost. Result looks like serviceName-cost
     *
     * @param serviceName
     * @param cost
     * @return
     */
    public static String buildService(String serviceName, float cost) {
        return serviceName.concat(CommonConstant.HYPHEN).concat(String.valueOf(cost));
    }

    /**
     * concat array new services into current services string. Separated by semi-colon
     *
     * @param currentServices
     * @param newServices
     * @return
     */
    public static String concatService(String currentServices, String... newServices) {
        return currentServices.concat(CommonConstant.STR_SEMI_COLON).concat(Arrays.stream(newServices).collect(Collectors.joining(CommonConstant.STR_SEMI_COLON)));
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

    public static Instant getCurrentInstant() {
        return Instant.now();
    }

    public static String getPlayerOneNameBy(Team team) {
        if (availablePlayerNotNull(team.getPlayerOne())) {
            return team.getPlayerOne().getPlayer().getPlayerName();
        }
        return CommonConstant.EMPTY;
    }

    public static String getPlayerTwoNameBy(Team team) {
        if (availablePlayerNotNull(team.getPlayerTwo())) {
            return team.getPlayerTwo().getPlayer().getPlayerName();
        }
        return CommonConstant.EMPTY;
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

    public static boolean teamPlayersNotNull(Team team) {
        return team != null && availablePlayerNotNull(team.getPlayerOne()) && availablePlayerNotNull(team.getPlayerTwo());
    }

    public static boolean isTeamOne(String area) {
        return GameState.teamOne().contains(area);
    }

    public static boolean isTeamTwo(String area) {
        return GameState.teamTwo().contains(area);
    }

}
