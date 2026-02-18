package com.badminton.controller;

import com.badminton.requestmodel.SessionRequest;
import com.badminton.response.result.Result;
import com.badminton.response.result.SessionResult;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.ResponseConvertor;
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
    public ResponseEntity<Result<SessionResult>> checkCreateNewSession() {
        return ResponseConvertor.convert(sessionService.checkAndCreateNewSessionInDay());
    }

    @PostMapping(value = "/deleteSession")
    public ResponseEntity<Result<SessionResult>> deleteSession() {
        return ResponseConvertor.convert(sessionService.closeOutDateSession(new SessionRequest(false)));
    }

}
