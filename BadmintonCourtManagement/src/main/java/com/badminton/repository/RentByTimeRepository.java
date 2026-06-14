package com.badminton.repository;

import com.badminton.entity.RentByTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RentByTimeRepository extends JpaRepository<RentByTime, Integer> {

    Optional<RentByTime> findByCourtCourtIdAndState(int courtId, String state);

    List<RentByTime> findByAvailablePlayerAvaId(long avaId);

    List<RentByTime> findByCourtCourtId(int courtId);

    List<RentByTime> findByState(String state);
}
