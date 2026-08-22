package com.badminton.service;

import com.badminton.constant.GameType;
import com.badminton.response.CourtManagementResponse;
import com.badminton.response.RentByTimeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourtManagementInterface {
    @Autowired
    CourtServicesService courtService;
    @Autowired
    RentByTimeService rentByTimeService;

    public CourtManagementResponse getCourtManagement() {
        CourtManagementResponse courtManaResponse = courtService.getCourtManagement();
        List<RentByTimeResponse> rentByTimeResponses = rentByTimeService.getRentsBySessionScopeAndCourtIds(courtManaResponse.getGameDTOs().stream()
                .filter(g -> g.getGameType().equals(GameType.RENT.name()))
                .map(g -> parseCourtId(g.getCourt().getCourtId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet())
        );
        courtManaResponse.setRentByTimeResponses(rentByTimeResponses);
        return courtManaResponse;
    }

    /**
     * Safely parse courtId from String to Integer with exception handling
     *
     * @param courtId the court ID as String
     * @return Optional<Integer> containing the parsed ID, or empty if parsing fails
     */
    private Optional<Integer> parseCourtId(String courtId) {
        try {
            return Optional.of(Integer.parseInt(courtId));
        } catch (NumberFormatException e) {
            log.error("Failed to parse courtId: '{}', skipping this record", courtId, e);
            return Optional.empty();
        } catch (NullPointerException e) {
            log.error("CourtId is null, skipping this record", e);
            return Optional.empty();
        }
    }
}
