package com.badminton.requestmodel.player;

import com.badminton.requestmodel.ServiceRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request to add, update or remove one service line on an available player.
 * {@code avaId} addresses the row directly (used by flows that already hold the
 * available player, e.g. a rental); when absent the open session is searched by
 * {@code playerName}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAvaPlayerRentRequest {
    private String player;
    private ServiceRequest updateServiceRequest;
    private String endedTime;
    private float numTime;
}
