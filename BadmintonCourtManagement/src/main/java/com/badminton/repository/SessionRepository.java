package com.badminton.repository;

import com.badminton.entity.Session;
import com.badminton.repository.filter.SessionParam;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Integer> {
    List<Session> findByFromTimeLessThanAndIsActive(Instant currentTime, boolean isActive);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    List<Session> findByFromTimeLessThanAndToTimeIsNullAndIsActive(Instant currentTime, boolean isActive, Sort sortBy);

    @Query("SELECT s FROM Session s " + "WHERE (:#{#params.from} IS NULL OR s.fromTime >= :#{#params.from}) " + "AND (s.toTime <= COALESCE(:#{#params.to}, CURRENT_TIMESTAMP))")
    List<Session> findAllByParams(@Param("params") SessionParam params, Sort sort);

    @Query("""
                SELECT s FROM Session s
                WHERE (:startInclusive IS NULL OR s.fromTime >= :startInclusive)
                  AND (:endExclusive IS NULL OR s.fromTime < :endExclusive)
            """)
    Page<Session> findByFromTimeBetween(Instant startInclusive, Instant endExclusive, Pageable pageable);

    @Query("""
                SELECT count(s) FROM Session s
                WHERE (:startInclusive IS NULL OR s.fromTime >= :startInclusive)
                  AND (:endExclusive IS NULL OR s.fromTime < :endExclusive)
            """)
    long countByFromTimeBetween(Instant startInclusive, Instant endExclusive);

    @Override
    Page<Session> findAll(Pageable pageable);

    @Query("""
                SELECT DISTINCT
                       YEAR(s.fromTime),
                       MONTH(s.fromTime)
                FROM Session s
                WHERE s.fromTime IS NOT NULL
                ORDER BY YEAR(s.fromTime) DESC, MONTH(s.fromTime) DESC
            """)
    List<Object[]> findDistinctYearMonthFromSessions();
}
