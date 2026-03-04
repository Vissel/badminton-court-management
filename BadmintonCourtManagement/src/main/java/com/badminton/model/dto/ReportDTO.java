package com.badminton.model.dto;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.entity.Session;
import com.badminton.util.TimeUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class ReportDTO {
    private Session session;
    private List<AvailablePlayer> availablePlayers;
    private List<Game> games;
    private List<ReportCost> listCost;

    public ReportDTO(Session session, List<AvailablePlayer> availablePlayers, List<Game> games) {
        this.session = session;
        this.availablePlayers = availablePlayers;
        this.games = games;
//        convertToListCost(games);
    }

    private void convertToListCost(List<Game> games) {
        this.listCost = new ArrayList<>();
        for (Game game : games) {
            ReportCost rptCost = new ReportCost();
            rptCost.setTeam1(new TeamDTO(game.getTeamOne()));
            rptCost.setTeam2(new TeamDTO(game.getTeamTwo()));
            rptCost.setStart(TimeUtils.convertInstantToTimeStr(game.getCreatedDate()));
            rptCost.setEnd(TimeUtils.convertInstantToTimeStr(game.getEndedDate()));
            rptCost.setCourtName(game.getCourt().getCourtName());
            listCost.add(rptCost);
        }
    }
}
