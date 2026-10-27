package com.badminton.core.shuttleball;

import com.badminton.entity.ShuttleBall;
import com.badminton.repository.ShuttleBallRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Owns the shuttle-ball catalog rows. Other cores that need a ball (e.g. the game core
 * opening a court's game) ask this service instead of reaching for the repository.
 */
@Service
public class CoreShuttleBallService {

    @Autowired
    private ShuttleBallRepository ballRepo;

    /**
     * The active catalog row carrying this name and cost: the price list is deactivate + insert,
     * so a name can appear more than once and the first active row is the current one.
     */
    public Optional<ShuttleBall> findActiveBall(String shuttleName, float cost) {
        return ballRepo.findAllByShuttleNameAndCostAndIsActiveTrue(shuttleName, cost).stream().findFirst();
    }
}
