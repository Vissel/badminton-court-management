package com.badminton.controller;

import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.requestmodel.*;
import com.badminton.response.CourtManagementResponse;
import com.badminton.response.RentByTimeResponse;
import com.badminton.response.ServiceResponse;
import com.badminton.response.result.Result;
import com.badminton.response.result.ShuttleBallResponse;
import com.badminton.service.CourtManagementInterface;
import com.badminton.service.CourtServicesService;
import com.badminton.service.RentByTimeService;
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
    private CourtServicesService courtService;
    @Autowired
    CourtManagementInterface courtManagementInterface;
    @Autowired
    private RentByTimeService rentByTimeService;

    @GetMapping(value = "/getShuttleBalls")
    public ResponseEntity<List<ShuttleBallResponse>> getShuttleBalls() {
        List<ShuttleBallResponse> res = ballService.getListActiveShuttleBallDTOs();
        log.info("Size active shuttle balls is:{}", res.size());
        // Error cases are not handled
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/addListBallIntoCourt")
    public ResponseEntity<Boolean> addListBallIntoCourt(@RequestParam String courtId,
                                                        @RequestBody List<ShuttleBallRequest> listBall) {
        Boolean res = ballService.addListOfShuttleBallIntoCourt(Integer.valueOf(courtId), listBall);
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/changeBallQuantity")
    public ResponseEntity<Result<Boolean>> changeBallQuantity(@RequestParam String courtId,
                                                              @RequestBody ShuttleBallDTO ballDTO) {
        return ResponseConvertor.convert(ballService.changeShuttleBallQuantity(courtId, ballDTO));
    }

    @GetMapping(value = "/getServices")
    public ResponseEntity<List<ServiceResponse>> getServices() {
        List<ServiceResponse> res = courtService.getActiveServices();
        log.info("Size active services is:{}", res.size());
        // Error cases are not handled
        return ResponseEntity.ok().body(res);
    }

    @GetMapping(value = "/getAllActiveCourt")
    public ResponseEntity<List<CourtDTO>> getAllActiveCourt() {
        log.info("Received GET /getAllActiveCourt request");
        List<CourtDTO> res = courtService.getAllActiveCourts();
        log.info("Size of active courts is:{}", res.size());
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
    public ResponseEntity<CourtManagementResponse> getCourtManagement() {
        log.info("Received GET /getCourtManagement request");

        CourtManagementResponse courtManaDTO = courtManagementInterface.getCourtManagement();

        // Error cases are not handled
        return ResponseEntity.ok().body(courtManaDTO);
    }

    @PostMapping(value = "/addPlayer")
    public ResponseEntity<Result<Boolean>> addPlayerToAvailableSession(@RequestBody AddPlayerRequest request) {
        log.info("Adding player:{} with advanceAmount:{}", request.getPlayerName(), request.getAdvanceAmount());
        Result<Boolean> res = courtService
                .addPlayerToCurrentSession(request);
        log.info("Result is:{}", res);
        // Error cases are not handled
        return ResponseConvertor.convert(res);
    }

    @PostMapping(value = "/updateAvailablePlayer")
    public ResponseEntity<Result<Boolean>> updateAvailablePlayer(@RequestBody AvaPlayerDTO avaPlayerDTO) {
        log.info("Updating available player from:{} to:{}", avaPlayerDTO.getOldPlayerName(),
                avaPlayerDTO.getPlayerName());
        Result<Boolean> res = courtService.updateAvailablePlayer(avaPlayerDTO);
        log.info("Result is:{}", res);
        return ResponseConvertor.convert(res);
    }

    @PostMapping(value = "/removeServiceOutPlayer")
    public ResponseEntity<Boolean> removeServiceOutAvaPlayer(@RequestParam String playerName,
                                                             @RequestBody ServiceDTO serviceDTO) {
        Boolean res = courtService.removeServiceOutAvailablePlayer(serviceDTO, playerName);
        // Error cases are not handled
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/updateServiceToPlayer")
    public ResponseEntity<Boolean> updateServiceToAvaPlayer(@RequestParam String playerName,
                                                            @RequestBody List<ServiceRequest> listServiceDTO) {
        Boolean res = courtService.updateServicesToAvailablePlayer(listServiceDTO, playerName);
        // Error cases are not handled
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/addServiceToPlayer")
    public ResponseEntity<Boolean> addServiceToAvaPlayer(@RequestParam String playerName,
                                                         @RequestBody ServiceRequest serviceRequest) {
        Boolean res = courtService.addServiceToAvailablePlayer(serviceRequest, playerName);
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
    public ResponseEntity<Boolean>
    changeGameState(@RequestBody GameDTO gameDTO) {
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

    @PostMapping(value = "/applyRentByTime")
    public ResponseEntity<RentByTimeResponse> applyRentByTime(@RequestBody RentByTimeRequest request) {
        RentByTimeResponse res = rentByTimeService.applyRentByTime(request);
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/payRentByTime")
    public ResponseEntity<RentByTimeResponse> payRentByTime(@RequestParam int rentId,
                                                            @RequestParam(required = false) Float customFee) {
        RentByTimeResponse res = rentByTimeService.payRentByTime(rentId, customFee);
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/cancelRentByTime")
    public ResponseEntity<RentByTimeResponse> cancelRentByTime(@RequestParam int rentId) {
        RentByTimeResponse res = rentByTimeService.cancelRentByTime(rentId);
        return ResponseEntity.ok().body(res);
    }

    @PostMapping(value = "/updateRentByTime")
    public ResponseEntity<RentByTimeResponse> updateRentByTime(@RequestParam int rentId,
                                                               @RequestBody RentByTimeRequest request) {
        RentByTimeResponse res = rentByTimeService.updateRentByTime(rentId, request);
        return ResponseEntity.ok().body(res);
    }

    @GetMapping(value = "/getActiveRentByTime")
    public ResponseEntity<RentByTimeResponse> getActiveRentByTime(@RequestParam int courtId) {
        return rentByTimeService.getActiveRentByTimeForCourt(courtId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok().body(null));
    }

    @GetMapping(value = "/getCurrentTime")
    public ResponseEntity<java.time.Instant> getCurrentTime() {
        return ResponseEntity.ok().body(rentByTimeService.getCurrentDbTime());
    }

}
