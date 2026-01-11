package com.badminton.service;

import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Session;
import com.badminton.repository.SessionRepository;
import com.badminton.repository.filter.SessionParam;
import com.badminton.requestmodel.Pagination;
import com.badminton.response.result.SessionResult;
import com.badminton.util.Converter;
import com.badminton.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Instant current = getMatchDBInstant();

        List<Session> availableSession = sessionRepo.findByFromTimeLessThanAndToTimeIsNullAndIsActive(current, true,
                Sort.by(Order.desc("sessionId")));
        if (!availableSession.isEmpty()) {
            ZoneId zone = ZoneId.of("Z");
            // DB's return global zone
            ZonedDateTime zoneDateT = availableSession.getFirst().getFromTime().atZone(zone);
            return zoneDateT.toLocalDate().equals(getMatchDBInstant().atZone(zone).toLocalDate());
        }
        return false;
    }

    @Transactional
    public SessionResult checkAndCreateNewSessionInDay() {
        Boolean available = checkAvailableSession();
        if (!available) {
            Session createdSession = new Session();
            sessionRepo.save(createdSession);
            return createdSessionResult(createdSession);
        }
        return availableSessionResult();
    }

    private SessionResult availableSessionResult() {
        SessionResult res = new SessionResult();
        res.setMessage("Session is available.");
        return res;
    }

    private SessionResult createdSessionResult(Session createdSession) {
        SessionResult res = new SessionResult();
        res.setId(createdSession.getSessionId());
        res.setFromTime(createdSession.getFromTime());
        return res;
    }

    /**
     * Schedule job will call
     *
     * @return
     */
    @Deprecated
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

    @Transactional
    public Boolean deactivateSessions() {
        List<Session> currSessions = findListCurrentSession();
        if (!currSessions.isEmpty()) {
            Instant toTime = Instant.now();
            currSessions.stream().forEach(s -> {
                s.setActive(false);
                s.setToTime(toTime);
            });
            sessionRepo.saveAll(currSessions);
        }
        return Boolean.TRUE;
    }

    public List<Session> findListCurrentSession() {
        Instant current = getMatchDBInstant();
        return sessionRepo.findByFromTimeLessThanAndToTimeIsNullAndIsActive(current, true,
                Sort.by(Order.desc("sessionId")));
    }

    @Transactional
    public AvailablePlayer getAvailablePlayerInActiveSession(String playerName) {
        Session currSession = findListCurrentSession().getFirst();
        return currSession.getAvailablePlayers().stream().filter(p -> p.getPlayer().getPlayerName().equals(playerName))
                .findFirst().orElse(null);
    }

    public List<Session> findListSessionBy(String yearMonthString, Pagination pagination) {
//        Pageable pageable = PageRequest.of(pagination.getCurrent(), pagination.getPageSize(), Sort.by(Sort.Direction.DESC, "fromTime"));
        Page<Session> pageSessions;
//        if (!StringUtils.isNoneBlank(yearMonthString) || "Tất cả".equals(yearMonthString)) {
//            pageSessions = sessionRepo.findAll(pageable);
//        } else {

        SessionParam sessionParam = buildSessionParams(yearMonthString, pagination);

        pageSessions = sessionRepo.findByFromTimeBetween(sessionParam.getFrom(), sessionParam.getTo(), sessionParam.getPageable());

        return pageSessions.stream().toList();
    }

    private Pageable buildPageableFrom(Pagination pagination) {
        return PageRequest.of(pagination.getCurrent(), pagination.getPageSize(), Sort.by(Sort.Direction.DESC, "fromTime"));
    }

    public long countSessionBy(String yearMonthString, Pagination pagination) {
        SessionParam sessionParam = buildSessionParams(yearMonthString, pagination);
        return sessionRepo.countByFromTimeBetween(sessionParam.getFrom(), sessionParam.getTo());
    }

    private SessionParam buildSessionParams(String yearMonthString, Pagination pagination) {
        SessionParam sessionParam =
                TimeUtils.convertYearMonthToInstant(yearMonthString);

        sessionParam.setPageable(buildPageableFrom(pagination));
        return sessionParam;
    }

    public Session findSessionById(String sessionId) {
        return sessionRepo.findById(Integer.valueOf(sessionId)).orElse(null);
    }
}
