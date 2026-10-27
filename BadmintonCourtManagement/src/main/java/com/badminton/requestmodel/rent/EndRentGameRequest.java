package com.badminton.requestmodel.rent;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to close the active RENT game of a court. The rent-by-time business
 * sends this to the Game business instead of writing the Game row itself.
 */
@Getter
@Setter
@NoArgsConstructor
public class EndRentGameRequest {
    private String courtId;
    /** GameState value: "Finish" or "Cancel" */
    private String gameState;

    public EndRentGameRequest(String courtId, String gameState) {
        this.courtId = courtId;
        this.gameState = gameState;
    }
}
