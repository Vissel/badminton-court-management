package com.badminton.controller;

import com.badminton.requestmodel.*;
import com.badminton.response.result.Result;
import com.badminton.service.CourtServicesServiceImpl;
import com.badminton.service.ShuttleBallServiceImpl;
import com.badminton.util.CommonUtil;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/court-mana")
public class CourtManagementController {

    @Autowired
    private ShuttleBallServiceImpl ballService;
    @Autowired
    private CourtServicesServiceImpl courtService;

    @GetMapping(value = "/getShuttleBalls")
    public ResponseEntity<List<ShuttleBallDTO>> getShuttleBalls() {
        List<ShuttleBallDTO> res = ballService.getListActiveShuttleBallDTOs();
        log.info("Size active shuttle balls is:{}", res.size());
        // Error cases are not handled
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/addListBallIntoCourt")
    public ResponseEntity<Boolean> addListBallIntoCourt(@RequestParam String courtId, @RequestBody List<ShuttleBallDTO> listBall) {
        Boolean res = ballService.addListOfShuttleBallIntoCourt(Integer.valueOf(courtId), listBall);
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/changeBallQuantity")
    public ResponseEntity<Result<Boolean>> changeBallQuantity(@RequestParam String courtId, @RequestBody ShuttleBallDTO ballDTO) {
        return ResponseConvertor.convert(ballService.changeShuttleBallQuantity(courtId, ballDTO));
    }

    @GetMapping(value = "/getServices")
    public ResponseEntity<List<ServiceDTO>> getServices() {
        List<ServiceDTO> res = courtService.getActiveServices();
        log.info("Size active services is:{}", res.size());
        // Error cases are not handled
        return ResponseEntity.ok().body(res);
    }

    @GetMapping(value = "/getAvailablePlayers")
    public ResponseEntity<List<AvaPlayerDTO>> getAvailablePlayers() {
        long currentMilis = System.currentTimeMillis();
        log.info("Current time:{}", new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss").format(new Date(currentMilis)));
        List<AvaPlayerDTO> res = courtService.getCurrentAvailablePlayers();
        log.info("Available player list size:{}", res.size());
        // Error cases are not handled
        return ResponseEntity.ok().body(res);
    }

    @GetMapping(value = "/getCourtManagement")
    public ResponseEntity<CourtManagementDTO> getCourtManagement() {
        log.info("Received GET /getCourtManagement request");

        CourtManagementDTO courtManaDTO = courtService.getCourtManagement();

        // Error cases are not handled
        return ResponseEntity.ok().body(courtManaDTO);
    }

    @PostMapping(value = "/addPlayer")
    public ResponseEntity<Result<Boolean>> addPlayerToAvailableSession(@RequestBody String name) {
        log.info("Adding player:{}", name);
        Result<Boolean> res = courtService
                .addPlayerToCurrentSession(name);
        log.info("Result is:{}", res);
        // Error cases are not handled
        return ResponseConvertor.convert(res);
    }


    @PostMapping(value = "/addServiceToPlayer")
    public ResponseEntity<Boolean> addServiceToAvaPlayer(@RequestParam String playerName,
                                                         @RequestBody ServiceDTO serviceDTO) {
        Boolean res = courtService.addServiceToAvailablePlayer(serviceDTO, playerName);
        // Error cases are not handled
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/addPlayerToCourt")
    public ResponseEntity<Boolean> addAvaPlayerToCourt(@RequestBody GameDTO gameDTO) throws Exception {
        Boolean res = courtService.addAvailablePlayerToCourtArea(gameDTO.getPlayerName(), gameDTO.getCourt(),
                gameDTO.getShuttleBalls().getFirst());
        // Error cases are not handled
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/removePlayerFromCourt")
    public ResponseEntity<Boolean> removePlayerFromCourtArea(@RequestBody CourtDTO courtDTO) {
        if (CommonUtil.checkValidCourt(courtDTO)) {
            Boolean res = courtService.removeAvailablePlayerFromCourtArea(courtDTO);
            // Error cases are not handled
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.badRequest().body(Boolean.FALSE);
    }

    /**
     * Req5 - Change game state: Started, Finish, Cancel
     */
    @PostMapping(value = "/changeGameState")
    public ResponseEntity<Boolean> changeGameState(@RequestBody GameDTO gameDTO) {
        if (gameDTO != null && StringUtils.isNoneBlank(gameDTO.getGameState(), gameDTO.getCourt().getCourtId())) {
            Boolean res = courtService.changeGameState(gameDTO);
            // Error cases are not handled
            return ResponseEntity.ok(res);
        }
        return ResponseEntity.badRequest().body(Boolean.FALSE);
    }

    @PostMapping(value = "/changeSelectedBall")
    public ResponseEntity<Void> changeSelectedBall(@RequestBody ShuttleBallDTO shuttleBallDTO) {
        ballService.changeSelectedShuttleBall(shuttleBallDTO);
        // Error cases are not handled
        return ResponseEntity.ok().body(null);
    }

}
