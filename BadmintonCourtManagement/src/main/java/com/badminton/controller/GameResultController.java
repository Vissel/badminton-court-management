package com.badminton.controller;

import com.badminton.response.result.GameResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/gameResult")
public class GameResultController {

    @GetMapping("/getGameResult")
    public ResponseEntity<GameResult> getGameResult() {

        return ResponseEntity.ok(null);
    }

    @PostMapping("/confirmGameResult")
    public ResponseEntity<GameResult> confirmGameResult() {

        return ResponseEntity.ok(null);
    }

    @PostMapping("/rejectGameResult")
    public ResponseEntity<GameResult> rejectGameResult() {

        return ResponseEntity.ok(null);
    }

    @PostMapping("/editGameResult")
    public ResponseEntity<GameResult> editGameResult() {

        return ResponseEntity.ok(null);
    }
}
