package com.badminton.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badminton.entity.ShuttleBall;

@Repository
public interface ShuttleBallRepositoty extends JpaRepository<ShuttleBall, Integer> {
	List<ShuttleBall> findAllByIsActive(boolean isActive);

	Optional<ShuttleBall> findByShuttleNameAndIsActive(String shuttleName, boolean isActive);

	List<ShuttleBall> findAllByShuttleName(String shuttleName);
}
