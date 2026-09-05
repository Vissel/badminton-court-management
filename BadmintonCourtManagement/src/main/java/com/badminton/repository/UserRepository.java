package com.badminton.repository;

import com.badminton.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Player, Integer> {

    Optional<Player> findByPlayerName(String username);

    List<Player> findAllByPlayerName(String name);


}
