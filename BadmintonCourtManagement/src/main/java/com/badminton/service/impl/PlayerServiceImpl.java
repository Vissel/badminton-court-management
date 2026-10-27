package com.badminton.service.impl;

import com.badminton.core.player.CoreAvailablePlayerService;
import com.badminton.core.player.CorePlayerService;
import com.badminton.entity.Player;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.player.UpdatePlayerDTO;
import com.badminton.requestmodel.player.UpdateAvaPlayerRentRequest;
import com.badminton.response.player.PlayerResponse;
import com.badminton.response.result.Result;
import com.badminton.service.PlayerService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerServiceImpl implements PlayerService {

    @Autowired
    private ServiceTemplate serviceTemplate;

    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private CoreAvailablePlayerService coreAvailablePlayerService;

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

    /**
     * Add a service line to the available player, or update its cost when a line
     * with the same service name already exists. Stock movements are only written
     * for lines that carry a stock item id.
     */
    @Override
    public Result<Boolean> updatePlayerService(UpdateAvaPlayerRentRequest request) {
        return serviceTemplate.execute(new ProcessCallback<UpdateAvaPlayerRentRequest, Boolean>() {

            @Override
            public UpdateAvaPlayerRentRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(UpdateAvaPlayerRentRequest req) {
                Assert.notNull(req, "Player service request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(req.getPlayer()), "Player name must not be blank.");
                Assert.notNull(req.getUpdateServiceRequest(), "Update service request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(req.getUpdateServiceRequest().getServiceName()), "Service name must not be blank.");
                Assert.isTrue(Float.valueOf(req.getUpdateServiceRequest().getCost()) >= 0, "Service cost must not be negative.");
            }

            @Override
            public Boolean process() {
                coreAvailablePlayerService.updateAvaPlayer(convertToAvaPlayerDTO(request));
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Remove the service line carrying the requested name from the available player.
     * A player that has no such line is left untouched.
     */
    @Override
    public Result<Boolean> removePlayerService(UpdateAvaPlayerRentRequest request) {
        return serviceTemplate.execute(new ProcessCallback<UpdateAvaPlayerRentRequest, Boolean>() {

            @Override
            public UpdateAvaPlayerRentRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(UpdateAvaPlayerRentRequest req) {
                Assert.notNull(req, "Player service request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(req.getPlayer()), "Player name must not be blank.");
                Assert.notNull(req.getUpdateServiceRequest(), "Update service request must not be null.");
                Assert.isTrue(StringUtils.isNotBlank(req.getUpdateServiceRequest().getServiceName()), "Service name must not be blank.");
            }

            @Override
            public Boolean process() {
                coreAvailablePlayerService.removeAvaPlayer(convertToAvaPlayerDTO(request));
                return Boolean.TRUE;
            }
        });
    }

    /**
     * The available player is addressed by name; the request carries one service line.
     */
    private UpdatePlayerDTO convertToAvaPlayerDTO(UpdateAvaPlayerRentRequest request) {
        UpdatePlayerDTO updatePlayerDTO = new UpdatePlayerDTO();
        updatePlayerDTO.setPlayerName(request.getPlayer());
        updatePlayerDTO.setListServiceDTO(Arrays.asList(
                new ServiceDTO(request.getUpdateServiceRequest().getServiceName(),
                        Float.valueOf(request.getUpdateServiceRequest().getCost()))));
        return updatePlayerDTO;
    }
}
