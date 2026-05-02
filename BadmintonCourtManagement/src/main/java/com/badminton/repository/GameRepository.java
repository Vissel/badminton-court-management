package com.badminton.repository;

import com.badminton.entity.Court;
import com.badminton.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GameRepository extends JpaRepository<Game, Integer> {
    Optional<Game> findByCourtAndStateAndEndedDateIsNull(Court court, String stateString);

    List<Game> findAllByStateInAndEndedDateIsNull(Set<String> stateIncludes);

    @Query("select g from Game g where g.court.courtId = ?1 and g.endedDate is null")
    Optional<Game> findByCourtIdAndEndedDateIsNull(int idOfCourt);

    @Query("select g from Game g where g.court.courtId = ?1 and g.endedDate is null")
    List<Game> findAllByCourtIdAndEndedDateIsNull(int idOfCourt);

    @Query("SELECT g FROM Game g " +
            "JOIN FETCH g.court c " +
            "LEFT JOIN FETCH g.teamOne t1 " +
            "LEFT JOIN FETCH g.teamTwo t2 " +
            "WHERE (t1.playerOne.avaId IN :avaIds OR t1.playerTwo.avaId IN :avaIds " +
            "OR t2.playerOne.avaId IN :avaIds OR t2.playerTwo.avaId IN :avaIds)")
    List<Game> findGamesByPlayerIds(@Param("avaIds") List<Long> avaIds);
}
