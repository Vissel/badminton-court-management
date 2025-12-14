package com.badminton.controller;

import com.badminton.requestmodel.GameDTO;
import com.badminton.response.result.GameResult;
import com.badminton.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/gameResult")
public class GameResultController {

    @Autowired
    GameService gameService;

    @GetMapping("/getGameResult")
    public ResponseEntity<GameResult> getGameResult(@RequestParam(name = "courtId") String courtId) {
        GameResult gameRes = gameService.getGameResult(Integer.valueOf(courtId));
        return ResponseEntity.ok(gameRes);
    }

    @PostMapping("/confirmGameResult")
    public ResponseEntity<GameResult> confirmGameResult(@RequestBody GameDTO gameDTO) {
        gameService.handleFinishGame(gameDTO);
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
