package com.badminton.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.badminton.entity.Team;

public interface TeamRepository extends JpaRepository<Team, Integer> {

}
