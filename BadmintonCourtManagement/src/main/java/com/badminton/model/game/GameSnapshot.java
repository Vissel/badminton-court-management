package com.badminton.model.game;

import lombok.Data;

import java.time.Instant;

/**
 * Holds the mutable fields of an active {@link com.badminton.entity.Game} captured
 * before a state transition (e.g. finish) so the change can be reverted as a
 * compensating action when a downstream step fails and rolls back its own transaction.
 */
@Data
public class GameSnapshot {
    private int gameId;
    private String state;
    private Instant endedDate;
    private String gtype;
    // Prior team expenses and win flags (null => leave the team untouched on revert)
    private Float teamOneExpenseOne;
    private Float teamOneExpenseTwo;
    private Boolean teamOneWin;
    private Float teamTwoExpenseOne;
    private Float teamTwoExpenseTwo;
    private Boolean teamTwoWin;
}
