package com.badminton.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badminton.entity.Court;

@Repository
public interface CourtRepositoty extends JpaRepository<Court, Integer> {

	List<Court> findAllByIsActive(boolean isActive);

	List<Court> findAllByIsActiveTrueAndCourtIdNotIn(Set<Integer> ids);

}
