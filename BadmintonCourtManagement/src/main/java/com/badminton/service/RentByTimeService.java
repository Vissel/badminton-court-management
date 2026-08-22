package com.badminton.service;

import com.badminton.constant.ApiConstant;
import com.badminton.constant.GameState;
import com.badminton.constant.GameType;
import com.badminton.constant.RentState;
import com.badminton.entity.AvailablePlayer;
import com.badminton.entity.Court;
import com.badminton.entity.RentByTime;
import com.badminton.model.dto.RentShuttleDTO;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.*;
import com.badminton.requestmodel.CourtDTO;
import com.badminton.requestmodel.GameDTO;
import com.badminton.requestmodel.RentByTimeRequest;
import com.badminton.response.RentByTimeResponse;
import com.badminton.time.model.SessionScope;
import com.badminton.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
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

    private static final String RENT_BY_TIME_PREFIX = "Thuê theo giờ ";

    @Autowired
    private RentByTimeRepository rentByTimeRepo;
    @Autowired
    private CourtRepositoty courtRepo;
    @Autowired
    private AvailablePlayerRepository avaPlayerRepo;
    @Autowired
    private ServiceRepositoty serviceRepo;

    @Autowired
    private SessionServiceImpl sessionService;

    @Autowired
    private CourtServicesService courtService;
    @Autowired
    private GameRepository gameRepo;

    @Autowired
    private ShuttleBallServiceImpl shuttleBallService;

    private BigDecimal getHourlyRate() {
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

        if (!gameRepo.findByCourtIdAndEndedDateIsNullAndGameStateNotStart(court.getCourtId(), GameState.NOT_START.getValue()).isPresent()) {
            throw new IllegalArgumentException("Game has NOT available");
        }

        getActiveRentByTimeForCourt(court.getCourtId())
                .ifPresent(r -> {
                    throw new IllegalArgumentException("Court already has an active rental");
                });

        Instant startTime = sessionService.getUTCPlus7Instant();
        BigDecimal numTime = BigDecimal.valueOf(request.getNumTime()).setScale(2, RoundingMode.HALF_UP);

        Instant endTime = startTime.plus(Duration.ofMinutes(numTime.multiply(BigDecimal.valueOf(60)).longValue()));

        String shuttlesJson = buildShuttlesJson(request.getShuttleBalls());

        RentByTime rental = new RentByTime(player, court, startTime, endTime, numTime, shuttlesJson,
                RentState.STARTED.name());
        rentByTimeRepo.save(rental);
        GameDTO gameDTO = buildGameDTO(request);
        courtService.changeGameState(gameDTO);

        // Add service to player
        BigDecimal hourlyRate = getHourlyRate();
        BigDecimal cost = numTime.multiply(hourlyRate).setScale(0, RoundingMode.HALF_UP);
        String serviceName = RENT_BY_TIME_PREFIX + court.getCourtName();
        ServiceDTO serviceDTO = new ServiceDTO();
        serviceDTO.setServiceName(serviceName);
        serviceDTO.setCost(cost.floatValue());

        // player.setServices(ServiceUtil.addServiceToJsonArray(player.getCurrentServices(),
        // serviceDTO));
        avaPlayerRepo.save(player);

        return toResponse(rental);
    }

    private GameDTO buildGameDTO(RentByTimeRequest request) {
        CourtDTO courtDTO = new CourtDTO();
        courtDTO.setCourtId(request.getCourtId());
        return new GameDTO(request.getPlayerName(), courtDTO, request.getShuttleBalls(), "Rent", null, "Start");
    }

    @Transactional
    public RentByTimeResponse payRentByTime(int rentId, Float customFee) {
        RentByTime rental = rentByTimeRepo.findById(rentId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        // Update player's service cost to include shuttle costs
        AvailablePlayer player = rental.getAvailablePlayer();
        String serviceName = RENT_BY_TIME_PREFIX + rental.getCourt().getCourtName();
        BigDecimal courtFee = customFee != null
                ? BigDecimal.valueOf(customFee).setScale(0, RoundingMode.HALF_UP)
                : (rental.getNumTime() != null
                ? rental.getNumTime().multiply(getHourlyRate()).setScale(0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        BigDecimal shuttleCost = calculateShuttleCost(rental.getShuttles());
        BigDecimal totalCost = courtFee.add(shuttleCost);

        List<ServiceDTO> services = ServiceUtil.convertStringToListService(player.getCurrentServices());
        // for (ServiceDTO s : services) {
        // if (s.getServiceName().equals(serviceName)) {
        // s.setCost(totalCost.floatValue());
        // break;
        // }
        // }
        services.add(new ServiceDTO(serviceName, totalCost.floatValue()));
        player.setServices(ServiceUtil.buildJsonArrayStr(services));
        avaPlayerRepo.save(player);

        rental.setState(RentState.FINISH.name());
        Instant now = sessionService.getUTCPlus7Instant();
        if (rental.getEndTime() == null || now.isAfter(rental.getEndTime())) {
            rental.setEndTime(now);
        }
        rentByTimeRepo.save(rental);
        updateRentGameState(rental.getCourt(), GameState.FINISH);
        return toResponse(rental);
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
    public RentByTimeResponse cancelRentByTime(int rentId) {
        RentByTime rental = rentByTimeRepo.findById(rentId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));
        rental.setState(RentState.CANCEL.name());
        rental.setEndTime(sessionService.getUTCPlus7Instant());
        rentByTimeRepo.save(rental);
        updateRentGameState(rental.getCourt(), GameState.CANCEL);

        // Remove rentByTime service from player
        AvailablePlayer player = rental.getAvailablePlayer();
        String serviceName = RENT_BY_TIME_PREFIX + rental.getCourt().getCourtName();
        List<ServiceDTO> services = ServiceUtil.convertStringToListService(player.getCurrentServices());
        List<ServiceDTO> filtered = services.stream()
                .filter(s -> !s.getServiceName().equals(serviceName))
                .collect(Collectors.toList());
        player.setServices(ServiceUtil.buildJsonArrayStr(filtered));
        avaPlayerRepo.save(player);

        return toResponse(rental);
    }

    @Transactional
    public RentByTimeResponse updateRentByTime(int rentId, RentByTimeRequest request) {
        RentByTime rental = rentByTimeRepo.findById(rentId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found"));

        // Apply startTime if provided
        if (request.getStartTime() != null && !request.getStartTime().isBlank()) {
            rental.setStartTime(Instant.parse(request.getStartTime()));
        }

        // Apply numTime if provided, and update player service cost
        if (request.getNumTime() > 0) {
            BigDecimal numTime = BigDecimal.valueOf(request.getNumTime()).setScale(2, RoundingMode.HALF_UP);
            rental.setNumTime(numTime);

            AvailablePlayer player = rental.getAvailablePlayer();
            String serviceName = RENT_BY_TIME_PREFIX + rental.getCourt().getCourtName();
            BigDecimal hourlyRate = getHourlyRate();
            BigDecimal cost = numTime.multiply(hourlyRate).setScale(0, RoundingMode.HALF_UP);
            List<ServiceDTO> services = ServiceUtil.convertStringToListService(player.getCurrentServices());
            boolean updated = false;
            for (ServiceDTO s : services) {
                if (s.getServiceName().equals(serviceName)) {
                    s.setCost(cost.floatValue());
                    updated = true;
                    break;
                }
            }
            if (updated) {
                player.setServices(ServiceUtil.buildJsonArrayStr(services));
                avaPlayerRepo.save(player);
            }
        }

        // Apply endTime if provided (frontend has already done cross-field correction)
        if (request.getEndTime() != null && !request.getEndTime().isBlank()) {
            rental.setEndTime(Instant.parse(request.getEndTime()));
        }

        // Apply shuttles if provided
        if (request.getShuttleBalls() != null) {
            rental.setShuttles(buildShuttlesJson(request.getShuttleBalls()));
        }

        rentByTimeRepo.save(rental);
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

    private void updateRentGameState(Court court, GameState targetState) {
        gameRepo.findByCourtIdAndEndedDateIsNull(court.getCourtId())
                .filter(g -> GameType.RENT.name().equals(g.getGtype()))
                .ifPresent(game -> {
                    game.setState(targetState.getValue());
                    game.setEndedDate(sessionService.getUTCPlus7Instant());
                    gameRepo.save(game);
                });
    }

    private String buildShuttlesJson(List<ShuttleBallDTO> shuttleBalls) {
        if (shuttleBalls == null || shuttleBalls.isEmpty()) {
            return null;
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
        res.setFee(rental.getNumTime() != null
                ? rental.getNumTime().multiply(getHourlyRate()).floatValue()
                : 0f);
        res.setShuttleBalls(ServiceUtil.convertShuttlesJsonToList(rental.getShuttles()));
        res.setState(rental.getState());

        Instant now = sessionService.getUTCPlus7Instant();
        if (rental.getEndTime() != null && now.isBefore(rental.getEndTime())) {
            res.setRemainingMinutes(Duration.between(now, rental.getEndTime()).toMinutes());
        } else {
            res.setRemainingMinutes(0);
        }
        return res;
    }
}
