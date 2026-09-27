package com.badminton.service;

import com.badminton.response.player.PlayerResponse;
import com.badminton.response.result.Result;

import java.util.List;

public interface PlayerService {

    Result<List<PlayerResponse>> getAllPlayers();
}
