package com.badminton.service;

import com.badminton.constant.*;
import com.badminton.core.rent.CoreRentByTimeService;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Court;
import com.badminton.entity.RentByTime;
import com.badminton.model.dto.RentByTimeDTO;
import com.badminton.model.dto.RentShuttleDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.CourtRepositoty;
import com.badminton.repository.RentByTimeRepository;
import com.badminton.repository.ServiceRepository;
import com.badminton.requestmodel.CourtDTO;
import com.badminton.requestmodel.GameDTO;
import com.badminton.requestmodel.player.UpdateAvaPlayerRentRequest;
import com.badminton.requestmodel.rent.CancelRentByTimeRequest;
import com.badminton.requestmodel.rent.EndRentByTimeRequest;
import com.badminton.requestmodel.rent.RentByTimeRequest;
import com.badminton.requestmodel.rent.RentRequest;
import com.badminton.response.rentbytime.RentByTimeResponse;
import com.badminton.response.result.Result;
import com.badminton.time.model.SessionScope;
import com.badminton.util.MoneyUtils;
import com.badminton.util.ServiceConverter;
import com.badminton.util.ServiceUtil;
import com.badminton.util.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RentByTimeService {

    @Autowired
    private CoreRentByTimeService coreRentByTimeService;
    @Autowired
    private RentByTimeRepository rentByTimeRepo;
    @Autowired
    private CourtRepositoty courtRepo;
    @Autowired
    private ServiceRepository serviceRepo;

    @Autowired
    private SessionServiceImpl sessionService;
    @Autowired
    private PlayerService playerService;
    @Autowired
    private CourtServicesService courtService;
    @Autowired
    private GameService gameService;


    private BigDecimal getRentByTimeServiceCost() {
        Optional<com.badminton.entity.Service> opt = serviceRepo.findBySerName(ApiConstant.RENT_BY_TIME);
        if (opt.isPresent() && opt.get().getCost() > 0) {
            return BigDecimal.valueOf(opt.get().getCost());
        }
        return new BigDecimal("100000"); // fallback default
    }

    @Transactional
    public RentByTimeResponse applyRentByTime(RentByTimeRequest request) {
        Court court = courtRepo.findById(Integer.valueOf(request.getCourtId()))
                .orElseThrow(() -> new IllegalArgumentException("Court not found"));

        AvailablePlayer player = sessionService.getAvailablePlayerInActiveSession(request.getPlayerName());
        if (player == null) {
            throw new IllegalArgumentException("Player not found in active session");
        }

        // Court availability is the court business's answer, built on the game core's read of
        // the court's open game.
        if (!Boolean.TRUE.equals(courtService.isCourtReadyForRent(court.getCourtId()))) {
            throw notRentable(court);
        }

        getActiveRentByTimeForCourt(court.getCourtId())
                .ifPresent(r -> {
                    throw new IllegalArgumentException("Court already has an active rental");
                });

        Instant startTime = sessionService.getUTCPlus7Instant();
        BigDecimal numTime = BigDecimal.valueOf(request.getNumTime()).setScale(2, RoundingMode.HALF_UP);
        Instant endTime = startTime.plus(Duration.ofMinutes(numTime.multiply(BigDecimal.valueOf(60)).longValue()));

        RentByTime rental = new RentByTime(player, court, startTime, endTime, numTime,
                buildShuttlesJson(request.getShuttleBalls()), RentState.STARTED.name());
        rentByTimeRepo.save(rental);

        // Start the RENT game through the Game business; its failure must roll this back.
        Result<Boolean> gameResult = gameService.handleChangeGameState(buildGameDTO(request, GameState.START));
        if (!gameResult.isSuccess()) {
            throw new IllegalStateException("Cannot start rent game: " + gameResult.getErrorMessage());
        }
        return toResponse(rental);
    }

    /**
     * A rent needs an open game in "Not start", so a refusal reports the states the court
     * actually carries. Without this the reason is only visible as "not available".
     */
    private IllegalArgumentException notRentable(Court court) {
        List<String> openStates = gameService.findOpenGameStates(court.getCourtId());
        log.warn("Rent refused on court {} ({}): open game states = {}",
                court.getCourtId(), court.getCourtName(), openStates);
        return new IllegalArgumentException(openStates.isEmpty()
                ? "Court " + court.getCourtName()
                        + " has no open game: put the renter on the court first."
                : "Court " + court.getCourtName() + " is [" + String.join(", ", openStates)
                        + "], but a rent needs [" + GameState.NOT_START.getValue() + "].");
    }

    private GameDTO buildGameDTO(RentRequest request, GameState gameState) {
        CourtDTO courtDTO = new CourtDTO();
        courtDTO.setCourtId(request.getCourtId());
        return new GameDTO(request.getPlayerName(), courtDTO, request.getShuttleBalls(),
                GameType.RENT.name(), null, gameState.getValue());
    }

    @Transactional
    public RentByTimeResponse finishRentByTime(EndRentByTimeRequest request) {
        RentByTime rental = coreRentByTimeService.updateRentByTime(
                convertToRentByTimeDtoForFinish(request));

        // AvailablePlayer business owns the player's service lines: the settled fee (the
        // client's override, or the fee the rental implies) is recorded as one line.
        Result<Boolean> serviceResult = playerService
                .updatePlayerService(buildRentServiceRequest(rental, resolveCourtFee(rental).floatValue()));
        if (!serviceResult.isSuccess()) {
            throw new IllegalStateException("Cannot record rent fee on player: " + serviceResult.getErrorMessage());
        }

        // Game business owns the game row.
        Result<Boolean> gameResult = gameService.handleFinishGame(
                buildGameDTO(request, GameState.FINISH));
        if (!gameResult.isSuccess()) {
            throw new IllegalStateException("Cannot finish rent game: " + gameResult.getErrorMessage());
        }
        return toFinishRentByTimeResponse(request.getCourtId(), request.getPlayerName());
    }

    private RentByTimeResponse toFinishRentByTimeResponse(String courtId, String playerName) {
        RentByTimeResponse finishRentByTimeResponse = new RentByTimeResponse();
        finishRentByTimeResponse.setCourtId(Integer.valueOf(courtId));
        finishRentByTimeResponse.setPlayerName(playerName);
        return finishRentByTimeResponse;
    }

    /**
     * The client's moment wins; without one a rental still never ends in the future, and a
     * value that is not a date/time at all is rejected instead of silently becoming "now".
     */
    private Instant resolveSettledAt(String endedTime, Instant bookedEndTime) {
        if (StringUtils.isNotBlank(endedTime)) {
            return TimeUtils.convertToInstant(endedTime);
        }
        Instant now = sessionService.getUTCPlus7Instant();
        return bookedEndTime == null || now.isAfter(bookedEndTime) ? now : bookedEndTime;
    }

    /**
     * Settle argument object: when the rental ended, how long it ran, the fee actually
     * charged and the note explaining how it differs from the booked fee.
     */
    private RentByTimeDTO convertToRentByTimeDtoForFinish(EndRentByTimeRequest request) {
        RentByTimeDTO dto = new RentByTimeDTO();
        dto.setRentId(request.getRentId());
        dto.setEndTime(TimeUtils.convertToInstant(
                request.getEndedTime()));
        dto.setNumTime(request.getNumTime());
        dto.setActualFee(toActualFee(request.getActualFee()));
        dto.setState(RentState.FINISH);
        return dto;
    }

    private RentByTimeDTO convertToRentByTimeDtoForCancel(CancelRentByTimeRequest request) {
        RentByTimeDTO dto = new RentByTimeDTO();
        dto.setRentId(request.getRentId());
        dto.setEndTime(TimeUtils.convertToInstant(
                request.getEndedTime()));
        dto.setState(RentState.CANCEL);
        return dto;
    }

    /**
     * System fee a rental implies: duration in hours * hourly rate.
     */
    private BigDecimal rentFee(BigDecimal numTime) {
        if (numTime == null) {
            return BigDecimal.ZERO;
        }
        return numTime.multiply(getRentByTimeServiceCost()).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Fee to bill for the court: the override the client sent, otherwise the system fee.
     */
    private BigDecimal resolveCourtFee(RentByTime rental) {
        return rental.getActualFee() != null ? rental.getActualFee() : rentFee(rental.getNumTime());
    }

    /**
     * A positive client amount is an override; anything else means "bill the system fee".
     */
    private BigDecimal toActualFee(float actualFee) {
        return actualFee > 0 ? BigDecimal.valueOf(actualFee).setScale(0, RoundingMode.HALF_UP) : null;
    }

    /**
     * Note explaining a manual fee override. Returns null when the actual fee matches
     * the fee the rental implies, so only genuine changes are documented.
     */
    private String buildFeeChangeNote(BigDecimal actualFee, BigDecimal systemFee) {
        if (actualFee == null || actualFee.compareTo(systemFee) == 0) {
            return null;
        }
        return String.format("Thu phí thực tế %1$sđ, khác phí hệ thống %2$sđ",
                MoneyUtils.formatToVND(actualFee.doubleValue()),
                MoneyUtils.formatToVND(systemFee.doubleValue()));
    }

    /**
     * Player-service argument object for a rental that is already stored: the player and the
     * service line name come from the row, so a settle, an update and a cancel all address
     * the same line on the same player.
     */
    private UpdateAvaPlayerRentRequest buildRentServiceRequest(RentByTime rental, float cost) {
        UpdateAvaPlayerRentRequest request = new UpdateAvaPlayerRentRequest();
        request.setPlayer(rental.getAvailablePlayer().getPlayer().getPlayerName());
        request.setUpdateServiceRequest(ServiceConverter.buildServiceRequest(ServiceConstants.RENT_BY_TIME, cost));
        return request;
    }

    private BigDecimal calculateShuttleCost(String shuttlesJson) {
        if (shuttlesJson == null || shuttlesJson.isBlank()) {
            return BigDecimal.ZERO;
        }
        List<RentShuttleDTO> shuttles = ServiceUtil.convertShuttlesJsonToList(shuttlesJson);
        return shuttles.stream()
                .map(s -> BigDecimal.valueOf(s.getCost() * s.getNumber()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);
    }

    @Transactional
    public RentByTimeResponse cancelRentByTime(CancelRentByTimeRequest cancelRentByTimeRequest) {
        // Cancelling the rental is a rent-row change like any other, so it goes through the core too.
        RentByTime rental = coreRentByTimeService.updateRentByTime(convertToRentByTimeDtoForCancel(cancelRentByTimeRequest));

        // AvailablePlayer business owns the player's service lines: the cancelled rental
        // leaves no fee behind.
        Result<Boolean> serviceResult = playerService
                .removePlayerService(buildRentServiceRequest(rental, 0f));
        if (!serviceResult.isSuccess()) {
            throw new IllegalStateException("Cannot remove rent fee from player: " + serviceResult.getErrorMessage());
        }

        // Game business owns the game row.
        Result<Boolean> gameResult = gameService.terminateGame(
                buildGameDTO(cancelRentByTimeRequest, GameState.CANCEL));
        if (!gameResult.isSuccess()) {
            throw new IllegalStateException("Cannot cancel rent game: " + gameResult.getErrorMessage());
        }
        return toResponse(rental);
    }

    @Transactional
    public RentByTimeResponse updateRentByTime(int rentId, RentByTimeRequest request) {
        RentByTimeDTO updateRentByTimeDTO = new RentByTimeDTO();
        updateRentByTimeDTO.setRentId(rentId);
        if (StringUtils.isNotBlank(request.getEndTime())) {
            updateRentByTimeDTO.setEndTime(TimeUtils.convertToInstant(request.getEndTime()));
        }
        updateRentByTimeDTO.setNumTime(request.getNumTime());
        if (request.getShuttleBalls() != null) {
            updateRentByTimeDTO.setShuttles(buildShuttlesJson(request.getShuttleBalls()));
        }

        RentByTime rental = coreRentByTimeService.updateRentByTime(updateRentByTimeDTO);

        // While the rental is running, its player line carries the court fee alone; the
        // shuttle cost is added when the rental is settled.
        if (RentState.STARTED.name().equals(rental.getState())) {
            Result<Boolean> serviceResult = playerService
                    .updatePlayerService(buildRentServiceRequest(rental, resolveCourtFee(rental).floatValue()));
            if (!serviceResult.isSuccess()) {
                throw new IllegalStateException("Cannot update rent fee on player: " + serviceResult.getErrorMessage());
            }
        }
        return toResponse(rental);
    }

    /**
     * Get all RentByTime by courtIds within session scope time range
     *
     * @param courtIds
     * @return
     */
    public List<RentByTimeResponse> getRentsBySessionScopeAndCourtIds(Set<Integer> courtIds) {
        SessionScope sessionScope = sessionService.getSessionScope();
        return rentByTimeRepo.findByCourtCourtIdInAndStartTimeAfterAndEndTimeBefore(
                courtIds,
                sessionScope.getStart(),
                sessionScope.getEnd()
        ).stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Optional<RentByTimeResponse> getActiveRentByTimeForCourt(int courtId) {
        return rentByTimeRepo.findByCourtCourtIdAndStateAndEndTimeIsNull(courtId, RentState.STARTED.name()).map(this::toResponse);
    }

    public Instant getCurrentDbTime() {
        return sessionService.getUTCPlus7Instant();
    }

    private String buildShuttlesJson(List<ShuttleBallDTO> shuttleBalls) {
        if (shuttleBalls == null) {
            return ServiceUtil.convertShuttlesListToJson(List.of());
        }
        List<RentShuttleDTO> list = shuttleBalls.stream().map(dto -> {
            RentShuttleDTO r = new RentShuttleDTO();
            r.setShuttleName(dto.getShuttleName());
            r.setCost(dto.getShuttleCost());
            r.setNumber(dto.getBallQuantity());
            return r;
        }).collect(Collectors.toList());
        return ServiceUtil.convertShuttlesListToJson(list);
    }

    private RentByTimeResponse toResponse(RentByTime rental) {
        RentByTimeResponse res = new RentByTimeResponse();
        res.setId(rental.getId());
        res.setCourtId(rental.getCourt().getCourtId());
        res.setCourtName(rental.getCourt().getCourtName());
        res.setPlayerName(rental.getAvailablePlayer().getPlayer().getPlayerName());
        res.setStartTime(rental.getStartTime());
        res.setEndTime(rental.getEndTime());
        res.setNumTime(rental.getNumTime() != null ? rental.getNumTime().floatValue() : 0f);
        res.setFee(rental.getActualFee() != null
                ? rental.getActualFee().floatValue()
                : rental.getNumTime() != null
                ? rental.getNumTime().multiply(getRentByTimeServiceCost()).floatValue()
                : 0f);
        res.setActualFee(rental.getActualFee() != null ? rental.getActualFee().floatValue() : null);
        res.setNote(rental.getNote());
        res.setShuttleBalls(ServiceUtil.convertShuttlesJsonToList(rental.getShuttles()));
        res.setState(rental.getState());

        Instant now = sessionService.getUTCPlus7Instant();
        if (rental.getEndTime() != null && now.isBefore(rental.getEndTime())) {
            Duration duration = Duration.between(now, rental.getEndTime());
            res.setRemainingMinutes((int) duration.toMinutes());
            res.setRemainingSeconds(duration.toSeconds() % 60);
        } else {
            res.setRemainingMinutes(0);
            res.setRemainingSeconds(0);
        }
        return res;
    }
}
