package com.badminton.service;

import com.badminton.requestmodel.player.UpdateAvaPlayerRentRequest;
import com.badminton.response.player.PlayerResponse;
import com.badminton.response.result.Result;

import java.util.List;

public interface PlayerService {

    Result<List<PlayerResponse>> getAllPlayers();

    Result<Boolean> updatePlayerService(UpdateAvaPlayerRentRequest updateAvaPlayerRentRequest);

    /**
     * Drop the service line with the requested name from the available player.
     */
    Result<Boolean> removePlayerService(UpdateAvaPlayerRentRequest updateAvaPlayerRentRequest);
}
