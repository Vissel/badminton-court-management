package com.badminton.dao;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Session;
import com.badminton.repository.AvailablePlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DaoPlayer {

    @Autowired
    AvailablePlayerRepository availablePlayerRepository;

    public List<AvailablePlayer> getAllAvailablePlayerBySessionAndName(Session session, String playerName) {
        return availablePlayerRepository
                .findAllAvailablePlayerInSessionByName(session, playerName);
    }
}
