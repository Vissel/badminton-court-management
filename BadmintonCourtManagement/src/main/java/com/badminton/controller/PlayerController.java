package com.badminton.controller;

import com.badminton.response.player.PlayerResponse;
import com.badminton.response.result.Result;
import com.badminton.service.PlayerService;
import com.badminton.util.ResponseConvertor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/player")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @GetMapping("/all")
    public ResponseEntity<Result<List<PlayerResponse>>> getAllPlayers() {
        return ResponseConvertor.convert(playerService.getAllPlayers());
    }
}
