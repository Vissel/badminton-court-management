package com.badminton.controller;

import com.badminton.response.result.SessionResult;
import com.badminton.service.SessionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    SessionServiceImpl sessionService;

    @GetMapping(value = "/checkAvailable")
    public ResponseEntity<Boolean> checkAvailableSession() {
        return ResponseEntity.ok(sessionService.checkAvailableSession());
    }

    @PostMapping(value = "/createNewSession")
    public ResponseEntity<Boolean> createNewSession() {
        return ResponseEntity.ok(sessionService.createNewSessionInDay());
    }

    @PostMapping(value = "/deleteSession")
    public ResponseEntity<List<SessionResult>> deleteSession() {
        return ResponseEntity.ok(sessionService.deactiveSessions());
    }

}
