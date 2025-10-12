package com.badminton.service;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Session;
import com.badminton.repository.SessionRepository;
import com.badminton.response.result.SessionResult;
import com.badminton.util.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
@Slf4j
public class SessionServiceImpl {
    @Autowired
    private SessionRepository sessionRepo;

    /**
     * DB display the time data ...
     *
     * @return
     */
    public Instant getMatchDBInstant() {
        // Create a Calendar instance
        Calendar calendar = Calendar.getInstance();
        // Plus 7 hours
        calendar.add(Calendar.HOUR, +7);
        log.info("Convert to DB time zone value: {}", calendar.toInstant().toString());
        return calendar.toInstant();
    }

    public Boolean checkAvailableSession() {
        List<Session> availableSession = findListCurrentSession();
        if (!availableSession.isEmpty()) {
            ZoneId zone = ZoneId.of("Z");
            // DB's return global zone
            ZonedDateTime zoneDateT = availableSession.getFirst().getFromTime().atZone(zone);
            return zoneDateT.toLocalDate().equals(getMatchDBInstant().atZone(zone).toLocalDate());
        }
        return false;
    }

    public Boolean createNewSessionInDay() {
        List<Session> avaSessions = findListCurrentSession();
        if (avaSessions.isEmpty()) {
            Session newSession = new Session();

            return sessionRepo.save(newSession).getSessionId() != 0;
        }
        return true;
    }

    /**
     * Schedule job will call
     *
     * @return
     */
    public List<SessionResult> deactiveSessions() {
        List<SessionResult> deactiveList = new ArrayList<>();
        List<Session> currSessions = findListCurrentSession();
        if (!currSessions.isEmpty()) {
            Instant toTime = Instant.now();
            currSessions.stream().forEach(s -> {
                s.setActive(false);
                s.setToTime(toTime);
                deactiveList.add(Converter.convertorToSessionResult(s));
            });
            sessionRepo.saveAll(currSessions);

        }
        return deactiveList;
    }

    public List<Session> findListCurrentSession() {
        Instant current = getMatchDBInstant();
        return sessionRepo.findByFromTimeLessThanAndToTimeIsNullAndIsActive(current, true,
                Sort.by(Order.desc("sessionId")));
    }

    public AvailablePlayer getAvailablePlayerInActiveSession(String playerName) {
        Session currSession = findListCurrentSession().getFirst();
        return currSession.getAvailablePlayers().stream().filter(p -> p.getPlayer().getPlayerName().equals(playerName))
                .findFirst().orElse(null);

    }

}
