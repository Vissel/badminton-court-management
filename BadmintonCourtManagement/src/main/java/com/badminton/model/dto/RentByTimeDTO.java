package com.badminton.model.dto;

import com.badminton.constant.RentState;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Argument object for updating a rent_by_time row. Every field is a patch: null (or
 * numTime &lt;= 0) means "keep the stored value", so a caller describes only what
 * actually changes and the same object serves a settle, a duration edit or a cancel.
 * <p>
 * Instants must already carry the DB convention (Vietnam wall clock kept as UTC) - build
 * them with {@link com.badminton.util.TimeUtils#convertToInstant(String)}.
 */
@Data
public class RentByTimeDTO {
    private int rentId;
    /**
     * new end moment; null = unchanged
     */
    private Instant endTime;
    /**
     * duration in hours; &lt;= 0 = unchanged
     */
    private float numTime;
    /**
     * serialised shuttle list; an empty JSON array clears it, null = unchanged
     */
    private String shuttles;
    /**
     * fee actually charged; null = unchanged
     */
    private BigDecimal actualFee;

    private RentState state;
    /**
     * reconciliation note; null = unchanged
     */
    private String note;
}
