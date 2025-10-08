package com.badminton.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Session;

public interface AvailablePlayerRepository extends JpaRepository<AvailablePlayer, Long> {

	@Query("select aplayer from AvailablePlayer aplayer where aplayer.session = ?1 AND aplayer.player.playerName = ?2 AND aplayer.leaveTime is NULL")
	AvailablePlayer findAvailablePlayerInSessionByName(Session session, String playerName);

	List<AvailablePlayer> findAllBySessionAndAvaIdNotInAndLeaveTimeIsNull(Session session, Set<Long> excludes);
}
