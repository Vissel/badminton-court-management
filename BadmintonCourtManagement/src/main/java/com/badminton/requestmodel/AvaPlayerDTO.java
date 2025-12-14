package com.badminton.requestmodel;

import com.badminton.constant.CommonConstant;
import com.badminton.entity.AvailablePlayer;
import com.badminton.util.CommonUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvaPlayerDTO extends ResponseDTO {
    private int playerId;
    private String playerName;
    @JsonIgnore
    private transient Instant from;
    @JsonIgnore
    private transient Instant to;
    private List<String> serviceDTOs;

    private float expense;

    public AvaPlayerDTO(AvailablePlayer avaPlayerEntity) {
        this.playerId = avaPlayerEntity.getPlayer().getPlayerId();
        this.playerName = avaPlayerEntity.getPlayer().getPlayerName();
        this.from = avaPlayerEntity.getSession().getFromTime();
        this.to = avaPlayerEntity.getSession().getToTime();
        this.serviceDTOs = new ArrayList<>();
        if (CommonUtil.isNotNullEmpty(avaPlayerEntity.getServices())) {
            this.serviceDTOs = Arrays.asList(avaPlayerEntity.getServices().split(CommonConstant.STR_SEMI_COLON))
                    .stream().filter(CommonUtil::isNotNullEmpty).collect(Collectors.toList());
        }
    }
}
