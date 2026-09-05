package com.badminton.core.player;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Player;
import com.badminton.entity.Session;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.SessionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class CoreAvailablePlayerService {
    @Autowired
    AvailablePlayerRepository availablePlayerRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SessionServiceImpl sessionService;

    @Transactional
    public Boolean updateAvailablePlayerName(String currName, String newName) {
        List<Session> activeSessions = sessionService.findListCurrentSession();
        if (activeSessions.isEmpty()) {
            return false;
        }
        log.info("Session is found");
        Session currentSession = activeSessions.getFirst();
        Optional<AvailablePlayer> availablePlayerOpt = availablePlayerRepository
                .findForUpdateAvailablePlayerInSessionByName(currentSession, currName);

        if (availablePlayerOpt.isPresent()) {
            AvailablePlayer availablePlayer = availablePlayerOpt.get();

            List<Player> players = userRepository.findAllByPlayerName(newName);
            Player newPlayer;
            if (!players.isEmpty()) {
                newPlayer = players.getFirst();
            } else {
                newPlayer = new Player(newName, newName);
                userRepository.save(newPlayer);
            }

            availablePlayer.setPlayer(newPlayer);
            availablePlayerRepository.save(availablePlayer);
            return true;
        }

        return false;
    }

    public AvailablePlayer getAvailablePlayerByName(Session session, String name) {
        return availablePlayerRepository.findAvailablePlayerInSessionByName(session, name).orElse(null);
    }

    public Player checkAvailableAndGetPlayer(Session session, String name) {
        AvailablePlayer availablePlayer = getAvailablePlayerByName(session, name);
        if (availablePlayer == null) {
            return null;
        }
        return userRepository.findByPlayerName(name).orElse(null);
    }
}
