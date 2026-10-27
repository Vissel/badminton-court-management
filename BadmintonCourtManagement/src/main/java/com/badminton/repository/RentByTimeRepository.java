package com.badminton.repository;

import com.badminton.constant.RentState;
import com.badminton.entity.RentByTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RentByTimeRepository extends JpaRepository<RentByTime, Integer> {
    /**
     * Find active RentByTime by courtId and state and endTime is null, active in session
     *
     * @param courtId
     * @param state
     * @return
     */
    Optional<RentByTime> findByCourtCourtIdAndStateAndEndTimeIsNull(int courtId, String state);

    List<RentByTime> findByAvailablePlayerAvaId(long avaId);

    List<RentByTime> findByCourtCourtId(int courtId);

    List<RentByTime> findByState(String state);

    /**
     * Find all RentByTime by courtIds
     *
     * @param courtIds
     * @return
     */
    List<RentByTime> findByCourtCourtIdIn(Set<Integer> courtIds);

    /**
     * Find all RentByTime by courtIds within session scope time range
     *
     * @param courtIds
     * @param sessionStart session start time
     * @param sessionEnd   session end time
     * @return
     */
    @Query("SELECT r FROM RentByTime r WHERE r.court.courtId IN :courtIds " +
            "AND r.startTime >= :sessionStart AND r.endTime <= :sessionEnd")
    List<RentByTime> findByCourtCourtIdInAndStartTimeAfterAndEndTimeBefore(
            @Param("courtIds") Set<Integer> courtIds,
            @Param("sessionStart") Instant sessionStart,
            @Param("sessionEnd") Instant sessionEnd);


    /**
     * Pessimistic write lock - the caller must be inside the rent transaction, so a
     * concurrent settle or cancel of one rental serializes on the row. The state column is
     * a plain varchar, so callers pass the text the database holds ({@link RentState#name()}),
     * not the enum.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RentByTime r WHERE r.id = :id AND r.state = :state")
    Optional<RentByTime> findByIdAndStateForUpdate(@Param("id") int id, @Param("state") String state);
}
