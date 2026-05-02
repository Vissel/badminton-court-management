package com.badminton.constant;

import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public enum GameState {
    NOT_START("Not start"), START("Start"), FINISH("Finish"), CANCEL("Cancel");

    private String value;

    GameState(String value) {
        this.value = value;
    }

    public static GameState getGameState(String gValue) {
        if (gValue.equals(NOT_START.value)) {
            return GameState.NOT_START;
        } else if (gValue.equals(START.value)) {
            return GameState.START;
        } else if (gValue.equals(FINISH.value)) {
            return GameState.FINISH;
        } else if (gValue.equals(CANCEL.value)) {
            return GameState.CANCEL;
        }
        return null;
    }

    public static Set<String> getNotTerminateState() {
        Set<String> res = new HashSet<>();
        res.add(NOT_START.value);
        res.add(START.value);
        return res;
    }

    public class Player {
        public static final String PLAYER_A = "A";
        public static final String PLAYER_B = "B";
        public static final String PLAYER_C = "C";
        public static final String PLAYER_D = "D";


    }

    public static List<String> teamOne() {
        return Arrays.asList(Player.PLAYER_A, Player.PLAYER_B);
    }

    public static List<String> teamTwo() {
        return Arrays.asList(Player.PLAYER_C, Player.PLAYER_D);
    }
}
