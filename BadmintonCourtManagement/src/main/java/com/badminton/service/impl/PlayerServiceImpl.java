package com.badminton.service.impl;

import com.badminton.core.player.CorePlayerService;
import com.badminton.entity.Player;
import com.badminton.response.player.PlayerResponse;
import com.badminton.response.result.Result;
import com.badminton.service.PlayerService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerServiceImpl implements PlayerService {

    @Autowired
    private ServiceTemplate serviceTemplate;

    @Autowired
    private CorePlayerService corePlayerService;

    @Override
    public Result<List<PlayerResponse>> getAllPlayers() {
        return serviceTemplate.execute(new ProcessCallback<Void, List<PlayerResponse>>() {
            @Override
            public Void getRequest() {
                return null;
            }

            @Override
            public void preProcess(Void request) {
            }

            @Override
            public List<PlayerResponse> process() {
                List<Player> players = corePlayerService.getAllPlayers();
                return players.stream()
                        .map(player -> new PlayerResponse(
                                player.getPlayerId(),
                                player.getPlayerName(),
                                player.getCreatedDate()))
                        .collect(Collectors.toList());
            }
        });
    }
}
