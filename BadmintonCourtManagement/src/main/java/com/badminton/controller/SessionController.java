package com.badminton.controller;

import com.badminton.response.result.SessionResult;
import com.badminton.service.SessionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/session")
public class SessionController {

    @Autowired
    SessionServiceImpl sessionService;

    @GetMapping(value = "/checkAvailable")
    public ResponseEntity<Boolean> checkAvailableSession() {
        return ResponseEntity.ok(sessionService.checkAvailableSession());
    }

    @PostMapping(value = "/checkCreateNewSession")
    public ResponseEntity<SessionResult> checkCreateNewSession() {
        return ResponseEntity.ok(sessionService.checkAndCreateNewSessionInDay());
    }

    @PostMapping(value = "/deleteSession")
    public ResponseEntity<Boolean> deleteSession() {
        return ResponseEntity.ok(sessionService.deactivateSessions());
    }

}
