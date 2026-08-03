package com.badminton.response;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Court;
import com.badminton.entity.Game;
import com.badminton.requestmodel.AvaPlayerDTO;
import com.badminton.requestmodel.CourtDTO;
import com.badminton.requestmodel.GameDTO;
import com.badminton.requestmodel.ResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class CourtManagementResponse extends ResponseDTO {
    private List<GameDTO> gameDTOs;
    private List<AvaPlayerDTO> availablePlayerDTOs;
    private List<CourtDTO> remainCourtDTOs;
    private List<CourtRentResponse> courtRents;

    public CourtManagementResponse() {
        this.gameDTOs = new ArrayList<>();
        this.availablePlayerDTOs = new ArrayList<>();
        this.remainCourtDTOs = new ArrayList<>();
    }

    public void convertToGameDTOs(List<Game> listOfGame) {
        this.gameDTOs = listOfGame.stream().map(g -> new GameDTO(g)).collect(Collectors.toList());
    }

    public void convertToAvaPlayerDTOs(List<AvailablePlayer> listOfAvaPlayer) {
        this.availablePlayerDTOs = listOfAvaPlayer.stream().map(p -> new AvaPlayerDTO(p)).collect(Collectors.toList());
    }

    public void convertToRemainCourtDTOs(List<Court> listOfCourt) {
        this.remainCourtDTOs = listOfCourt.stream().map(c -> new CourtDTO(c)).collect(Collectors.toList());
    }
}
