package com.badminton.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.badminton.entity.Player;

public interface UserRepository extends JpaRepository<Player, Integer> {

	Optional<Player> findByPlayerName(String username);
}
