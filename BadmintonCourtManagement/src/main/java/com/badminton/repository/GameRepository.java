package com.badminton.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.badminton.entity.Court;
import com.badminton.entity.Game;

public interface GameRepository extends JpaRepository<Game, Integer> {
	Optional<Game> findByCourtAndStateAndEndedDateIsNull(Court court, String stateString);

	List<Game> findAllByStateInAndEndedDateIsNull(Set<String> stateIncludes);

	@Query("select g from Game g where g.court.courtId = ?1 and g.endedDate is null")
	Optional<Game> findByCourtIdAndEndedDateIsNull(int idOfCourt);
}
