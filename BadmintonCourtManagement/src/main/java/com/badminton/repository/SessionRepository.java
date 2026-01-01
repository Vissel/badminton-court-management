package com.badminton.repository;

import com.badminton.entity.Session;
import com.badminton.repository.filter.SessionParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Integer> {
    List<Session> findByFromTimeLessThanAndIsActive(Instant currentTime, boolean isActive);

    List<Session> findByFromTimeLessThanAndToTimeIsNullAndIsActive(Instant currentTime, boolean isActive, Sort sortBy);

    @Query("SELECT s FROM Session s " +
            "WHERE (:#{#params.from} IS NULL OR s.fromTime >= :#{#params.from}) " +
            "AND (s.toTime <= COALESCE(:#{#params.to}, CURRENT_TIMESTAMP))")
    List<Session> findAllByParams(@Param("params") SessionParam params, Sort sort);

    Page<Session> findBySessionTimeBetween(
            Instant startInclusive,
            Instant endExclusive,
            Pageable pageable
    );

    @Override
    Page<Session> findAll(Pageable pageable);
}
