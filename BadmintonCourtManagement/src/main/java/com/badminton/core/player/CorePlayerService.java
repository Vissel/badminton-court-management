package com.badminton.core.player;

import com.badminton.entity.Player;
import com.badminton.model.PlayerModel;
import com.badminton.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CorePlayerService {

    @Autowired
    private UserRepository userRepository;

    public List<Player> getAllPlayers() {
        return userRepository.findAll();
    }

    public PlayerModel checkPlayerExistByUsername(String username) {
        return userRepository.findByPlayerName(username)
                .map(user -> new PlayerModel(user.getPlayerName()))
                .orElse(null);
    }
}
