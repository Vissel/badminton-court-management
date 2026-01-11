package com.badminton.repository;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Player;
import com.badminton.entity.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AvailablePlayerRepository extends JpaRepository<AvailablePlayer, Long> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select aplayer from AvailablePlayer aplayer where aplayer.session = ?1 AND aplayer.player.playerName = ?2 AND aplayer.leaveTime is NULL")
    Optional<AvailablePlayer> findAvailablePlayerInSessionByName(Session session, String playerName);

    List<AvailablePlayer> findAllBySessionAndAvaIdNotInAndLeaveTimeIsNull(Session session, Set<Long> excludes);

    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AvailablePlayer> findAllBySession(Session session);

    @Lock(LockModeType.PESSIMISTIC_READ)
    List<AvailablePlayer> findAllBySessionAndPlayerAndLeaveTimeIsNull(Session session, Player player);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AvailablePlayer> findAllForUpdateBySessionAndPlayerAndLeaveTimeIsNull(Session session, Player player);
}
