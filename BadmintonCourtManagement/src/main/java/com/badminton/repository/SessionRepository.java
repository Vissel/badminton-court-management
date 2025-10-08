package com.badminton.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.badminton.entity.Session;

public interface SessionRepository extends JpaRepository<Session, Integer> {
	List<Session> findByFromTimeLessThanAndIsActive(Instant currentTime, boolean isActive);

	List<Session> findByFromTimeLessThanAndToTimeIsNullAndIsActive(Instant currentTime, boolean isActive, Sort sortBy);
}
