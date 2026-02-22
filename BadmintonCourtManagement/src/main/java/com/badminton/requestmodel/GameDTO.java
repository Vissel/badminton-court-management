package com.badminton.requestmodel;

import com.badminton.entity.Game;
import com.badminton.entity.GameShuttleMap;
import com.badminton.model.dto.ShuttleBallDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameDTO extends ResponseDTO {
    private String playerName;
    private CourtDTO court;
    private List<ShuttleBallDTO> shuttleBalls;
//    private int ballQuantity;
    /**
     * this variable is used to display value from backend to frontend only.
     * Opposite side may have risks and not apply. << TODO
     */
    @JsonIgnore
    private Map<ShuttleBallDTO, Integer> shuttleMap;
    private String gameState;

    public GameDTO(Game game) {
//        this.shuttleBall = new ShuttleBallDTO(game.getShuttleBall());
//        this.ballQuantity = game.getShuttleNumber();
        shuttleMap = game.getShuttleMap().stream().collect(Collectors.toMap(
                m -> new ShuttleBallDTO(m.getShuttleBall()),
                GameShuttleMap::getShuttleNumber));

        this.gameState = game.getState();
        this.court = new CourtDTO(game.getCourt(), game.getTeamOne(), game.getTeamTwo());
    }
}
