package com.badminton.core.court;

import com.badminton.core.game.CoreGameService;
import com.badminton.entity.Court;
import com.badminton.exception.BusinessException;
import com.badminton.exception.validation.CourtCheck;
import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.repository.CourtRepositoty;
import com.badminton.requestmodel.CourtDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Owns the court row. Seating a player is a court request, but the rows it produces belong to
 * the game aggregate (the open game and its teams), so this service resolves the court and asks
 * the game core to do the seating - it never touches the game, team, ball or player repositories.
 */
@Slf4j
@Service
public class CoreCourtService {

    @Autowired
    private CourtRepositoty courtRepo;
    @Autowired
    private CoreGameService coreGameService;

    /**
     * Seat a player in the requested area of the court, opening the court's game when it has none.
     *
     * @return false when the court is unknown or a check rejects the seating (game not in
     * "Not start", shuttle ball not in the catalog, player not in the session)
     */
    public boolean addAvailablePlayerToCourtArea(String playerName, CourtDTO courtDTO, ShuttleBallDTO shuttleBallDTO) {
        try {
            Optional<Court> courtOpt = courtRepo.findById(Integer.valueOf(courtDTO.getCourtId()));
            CourtCheck.isCourtPresent(courtOpt);
            return coreGameService.addPlayerToCourt(courtOpt.get(), playerName,
                    courtDTO.getCourtAreas().getFirst().getArea(), shuttleBallDTO);
        } catch (BusinessException e) {
            log.warn("Adding {} to court {} is refused: {}", playerName, courtDTO.getCourtId(), e.getErrorMessage());
            return false;
        } catch (Exception e) {
            log.error("Error occurred while adding player to court area", e);
            return false;
        }
    }

    /**
     * Free the requested area of the court; the game core removes the court's game once its last
     * player is gone, so an empty court carries no game row.
     *
     * @return false when the court is unknown or a check rejects the removal (no game waiting for
     * players, no player in that area)
     */
    public boolean removeAvailablePlayerFromCourtArea(CourtDTO courtDTO) {
        try {
            Optional<Court> courtOpt = courtRepo.findById(Integer.valueOf(courtDTO.getCourtId()));
            CourtCheck.isCourtPresent(courtOpt);
            return coreGameService.removePlayerFromCourt(courtOpt.get(), courtDTO.getCourtAreas().getFirst().getArea());
        } catch (BusinessException e) {
            log.warn("Removing a player from court {} is refused: {}", courtDTO.getCourtId(), e.getErrorMessage());
            return false;
        } catch (Exception e) {
            log.error("Error occurred while removing player from court area", e);
            return false;
        }
    }
}
