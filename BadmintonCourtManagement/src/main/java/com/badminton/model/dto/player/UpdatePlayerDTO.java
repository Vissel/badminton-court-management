package com.badminton.model.dto.player;

import com.badminton.model.dto.ServiceDTO;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePlayerDTO {
    private String playerName;
    private List<ServiceDTO> listServiceDTO;
}
