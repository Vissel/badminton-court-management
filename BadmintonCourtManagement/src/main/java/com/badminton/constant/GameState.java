package com.badminton.constant;

import lombok.Getter;

@Getter
public enum GameState {
	NOT_START("Not start"), START("Start"), FINISH("Finish"), CANCEL("Cancel");

	private String value;

	private GameState(String value) {
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

	public class Player {
		public static final String PLAYER_A = "A";
		public static final String PLAYER_B = "B";
		public static final String PLAYER_C = "C";
		public static final String PLAYER_D = "D";
	}
}
