package com.badminton.dao;

import com.badminton.entity.Court;
import com.badminton.entity.Game;
import com.badminton.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DaoGame {
    @Autowired
    GameRepository gameRepository;

    public Optional<Game> findActiveGameByCourtId(String courtId) {
        return gameRepository.findByCourtIdAndEndedDateIsNull(Integer.valueOf(courtId));
    }

    public Optional<Game> findActiveGameByCourtIdForUpdate(String courtId) {
        return gameRepository.findByCourtAndEndedDateIsNull(new Court(Integer.valueOf(courtId)));
    }
}
