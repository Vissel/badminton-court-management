package com.badminton.core.player;

import com.badminton.entity.Player;
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
}
