package com.badminton.model;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Game;
import com.badminton.entity.Session;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReportDTO {
    private Session session;
    private List<AvailablePlayer> availablePlayers;
    private List<Game> games;
}
