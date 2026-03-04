package com.badminton.util;

import com.badminton.constant.CommonConstant;
import com.badminton.constant.GameConstant;
import com.badminton.constant.GameState;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.GameShuttleMap;
import com.badminton.entity.Team;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.requestmodel.CourtAreaDTO;
import com.badminton.response.result.ShuttleBallResponse;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    @Deprecated
    public static String buildService(String serviceName, float cost) {
        return serviceName.concat(CommonConstant.HYPHEN).concat(String.valueOf(cost));
    }

    public static List<ServiceDTO> convertStringToListService(String serviceString) {
        // Define the type of the target list using TypeToken
        Type objectListType = new TypeToken<ArrayList<ServiceDTO>>() {
        }.getType();
        List<ServiceDTO> dtos;
        try {
            dtos = new Gson().fromJson(serviceString, objectListType);
            Assert.notNull(dtos, "current service is null");
        } catch (JsonSyntaxException | IllegalArgumentException e) {
            dtos = new ArrayList<>();
        }
        return dtos;
    }

    public static <T> String buildJsonArrayStr(List<T> service) {
        return new Gson().toJson(service);
    }

    public static String addServiceToJsonArray(String existedService, ServiceDTO newService) {
        List<ServiceDTO> dtos = convertStringToListService(existedService);
        dtos.add(newService);
        return new Gson().toJson(dtos);
    }

    public static String divideServiceFromJsonArray(String existedService, ServiceDTO deletedService) {
        List<ServiceDTO> dtos = convertStringToListService(existedService);
        List<ServiceDTO> dividedList = dtos.stream().filter(s -> s.getServiceName().equals(deletedService.getServiceName()) && s.getCost() == deletedService.getCost()).collect(Collectors.toList());
        return new Gson().toJson(dividedList);
    }

    /**
     * concat array new services into current services string. Separated by semi-colon
     *
     * @param currentServices
     * @param newServices
     * @return
     */
    @Deprecated
    public static String concatService(String currentServices, String... newServices) {
        return currentServices.concat(CommonConstant.STR_SEMI_COLON).concat(Arrays.stream(newServices).collect(Collectors.joining(CommonConstant.STR_SEMI_COLON)));
    }

    /**
     * concat array new services into current services string. Separated by semi-colon
     *
     * @param currentServices
     * @param removedServices
     * @return
     */
    @Deprecated
    public static String divideService(String currentServices, String... removedServices) {
        Arrays.stream(removedServices).forEach(s -> currentServices.replace(CommonConstant.STR_SEMI_COLON + s, CommonConstant.EMPTY));
        return currentServices;
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
        return (current.equals(GameState.NOT_START) && change.equals(GameState.START)) || (current.equals(GameState.START) && isEndedState(change));
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

    public static Map<ShuttleBallResponse, Integer> retrievedShuttleBallMap(List<GameShuttleMap> gameShuttleMapping) {
        return gameShuttleMapping.stream().collect(Collectors.toMap(map -> new ShuttleBallResponse(map.getShuttleBall().getShuttleName(), map.getShuttleBall().getCost(), map.getShuttleBall().isSelected()), map -> Integer.valueOf(map.getShuttleNumber()), Integer::sum // merge function — sum counts if duplicates exist
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
