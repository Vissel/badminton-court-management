package com.badminton.util;

import com.badminton.constant.CommonConstant;
import com.badminton.constant.GameState;
import com.badminton.entity.Team;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
}
