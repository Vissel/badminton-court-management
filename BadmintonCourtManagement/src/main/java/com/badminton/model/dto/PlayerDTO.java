package com.badminton.model.dto;

import com.badminton.entity.AvailablePlayer;
import com.badminton.util.ServiceUtil;
import lombok.Data;

import java.util.List;

@Data
public class PlayerDTO {
    private long id;
    private String name;
    private List<ServiceDTO> listService;

    public PlayerDTO(AvailablePlayer player) {
        this.id = player.getAvaId();
        this.name = player.getPlayer().getPlayerName();
        this.listService = ServiceUtil.convertStringToListService(player.getServices());
    }
}
