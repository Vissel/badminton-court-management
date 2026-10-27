package com.badminton.core.session;

import com.badminton.entity.Session;
import com.badminton.model.session.SessionModel;
import com.badminton.repository.SessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;

@Slf4j
@Component
public class CoreSession {
    @Autowired
    private SessionRepository sessionRepo;

    /**
     * DB display the time data ...
     *
     * @return
     */
    public Instant getUTCPlus7Instant() {
        // Create a Calendar instance
        Calendar calendar = Calendar.getInstance();
        // Plus 7 hours
        calendar.add(Calendar.HOUR, +7);
        return calendar.toInstant();
    }

    /**
     * create new session if not available
     *
     * @return
     */
    @Transactional
    public com.badminton.model.session.SessionModel checkAndCreateNewSession() {
        if (findAvailableSessions(getUTCPlus7Instant()).isEmpty()) {
            return createNewSession();
        }
        return new SessionModel();
    }

    /**
     * Find available sessions
     *
     * @param fromCurrentTime
     * @return
     */
    public List<Session> findAvailableSessions(Instant fromCurrentTime) {
        return sessionRepo.findByFromTimeLessThanAndToTimeIsNullAndIsActive(fromCurrentTime, true,
                Sort.by(Sort.Order.desc("sessionId")));
    }

    public SessionModel createNewSession() {
        return toSessionModel(sessionRepo.save(new Session()));
    }

    private SessionModel toSessionModel(Session save) {
        SessionModel sessionModel = new SessionModel();
        sessionModel.setFromTime(save.getFromTime());
        sessionModel.setToTime(save.getToTime());
        return sessionModel;
    }

    public Session findCurrentSession() {
        return findListCurrentSession().getFirst();
    }

    public List<Session> findListCurrentSession() {
        Instant current = getUTCPlus7Instant();
        return findAvailableSessions(current);
    }

    public Session findSessionById(int sessionId) {
        return sessionRepo.findById(sessionId).get();
    }

    /**
     * Get all sessions for the date of the given date time (UTC+7 wall-clock),
     * ordered by fromTime desc.
     *
     * @param dateTime Date time string in ISO-8601 format (e.g.
     *                 "2026-09-05T10:00:00")
     * @return sessions for that day, or empty list if none
     */
    @Transactional(readOnly = true)
    public List<Session> findSessionsByDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return List.of();
        }

        try {
            LocalDateTime localDateTime = LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDate requestDate = localDateTime.toLocalDate();

            Instant startOfDay = requestDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant endOfDay = requestDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

            return sessionRepo.findByFromTimeBetweenOrderByFromTimeDesc(startOfDay, endOfDay);
        } catch (DateTimeParseException e) {
            log.error("Failed to parse dateTime: {}", dateTime, e);
            return List.of();
        }
    }
}
