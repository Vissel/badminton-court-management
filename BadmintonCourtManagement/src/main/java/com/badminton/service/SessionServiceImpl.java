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
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    public List<Session> findListSessionBy(String yearMonthString, Pagination pagination) {
        Pageable pageable = PageRequest.of(pagination.getPageSize(), pagination.getPageSize(), Sort.by("fromTime"));
        Page<Session> pageSessions;
        if (!StringUtils.isNoneBlank(yearMonthString) || "Tất cả".equals(yearMonthString)) {
            pageSessions = sessionRepo.findAll(pageable);
        } else {
            SessionParam sessionParam = TimeUtils.convertYearMonthToInstant(yearMonthString);
            pageSessions = sessionRepo.findBySessionTimeBetween(sessionParam.getFrom(), sessionParam.getTo(), pageable);
        }
        return pageSessions.stream().toList();
    }

//    public List<AvailablePlayer> getListAvailablePlayerInSessions(String yearMonthString) {
//        List<Session> listSession = findListSessionBy(yearMonthString);
//        return listSession.stream().flatMap(s -> s.getAvailablePlayers().stream()).collect(Collectors.toList());
//    }

//    public List<AvailablePlayer> getListAvailablePlayerInSessions(String fromStr, String toStr, String sortBy) {
//        SessionParam sessionParam = buildSessionParams(fromStr, toStr, sortBy);
//        Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
//        List<Session> listSession = sessionRepo.findBySessionTimeBetween(sessionParam.getFrom(), sessionParam.getTo(), null);
//        return listSession.stream().flatMap(s -> s.getAvailablePlayers().stream()).collect(Collectors.toList());
//    }

    private SessionParam buildSessionParams(String fromStr, String toStr, String sortBy) {
        SessionParam param = new SessionParam();
        Instant from = TimeUtils.convertToInstant(fromStr);
        Instant to = TimeUtils.convertToInstant(toStr);

        param.setFrom(from);
        param.setTo(to);
//        param.setOrderBy(new SessionParam.OrderBy());
        return param;
    }

}
