package com.badminton.requestmodel;

import com.badminton.entity.AvailablePlayer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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

    public AvaPlayerDTO(AvailablePlayer avaPlayerEntity) {
        this.playerId = avaPlayerEntity.getPlayer().getPlayerId();
        this.playerName = avaPlayerEntity.getPlayer().getPlayerName();
        this.from = avaPlayerEntity.getSession().getFromTime();
        this.to = avaPlayerEntity.getSession().getToTime();
    }
}
