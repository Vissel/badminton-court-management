package com.badminton.core.session;

import com.badminton.entity.Session;
import com.badminton.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SessionTransactionService {
    @Autowired
    private SessionRepository sessionRepo;

    @Transactional(readOnly = true)
    public List<Session> findAvailableSessions(Instant fromCurrentTime) {
        return sessionRepo.findByFromTimeLessThanAndToTimeIsNullAndIsActive(fromCurrentTime, true,
                Sort.by(Sort.Order.desc("sessionId")));
    }

    public void createNewAvailableSession() {
        sessionRepo.save(new Session());
    }
}
