package com.badminton.core.rent;

import com.badminton.constant.RentState;
import com.badminton.core.CoreBeanUtils;
import com.badminton.entity.RentByTime;
import com.badminton.model.dto.RentByTimeDTO;
import com.badminton.repository.RentByTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Owns every write to the rent_by_time row, the same way CoreGameService owns the game
 * row: callers describe the change with a {@link RentByTimeDTO} and never touch the
 * entity or its repository themselves.
 *
 * <p>Updates are applied by walking the properties of {@link RentByTime}, so a column is
 * updatable as soon as the argument object declares a field of the same name - this
 * service does not grow code per field.</p>
 */
@Service
public class CoreRentByTimeService {

    /**
     * num_time is decimal(4,2), so this is the longest rental the column can hold.
     */
    private static final BigDecimal MAX_NUM_TIME = new BigDecimal("99.99");
    private static final int NUM_TIME_SCALE = 2;
    private static final int MONEY_SCALE = 0;

    @Autowired
    private RentByTimeRepository rentByTimeRepo;
    @Autowired
    private CoreBeanUtils coreBeanUtils;

    /**
     * Read a rental exactly as stored, for callers that have to compare the booked values
     * with the ones they are about to write.
     */
    public RentByTime findById(int rentId) {
        return rentByTimeRepo.findById(rentId)
                .orElseThrow(() -> new IllegalArgumentException("Rental not found: " + rentId));
    }

    /**
     * Apply every property the argument object carries and persist the rental. A property
     * it does not carry - null for an object, zero for a primitive number - keeps its
     * stored value, and the stored row is validated before the write.
     * <p>
     * Only a rental that is still STARTED can be changed: the row is locked for update and
     * guarded on its state, so a repeated settle or cancel loses instead of rewriting an
     * already closed rental.
     *
     * @return the row as written, so the caller can answer with the settled values instead
     * of the ones it asked for.
     */
    @Transactional
    public RentByTime updateRentByTime(RentByTimeDTO rentByTimeDTO) {
        int rentId = rentByTimeDTO.getRentId();
        RentByTime rental = rentByTimeRepo.findByIdAndStateForUpdate(rentId, RentState.STARTED.name())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Rental " + rentId + " is not running any more, so it cannot be updated."));

        coreBeanUtils.setRentByTimePropertiesIfNotNull(rentByTimeDTO, rental);
        return rentByTimeRepo.save(rental);
    }


}
