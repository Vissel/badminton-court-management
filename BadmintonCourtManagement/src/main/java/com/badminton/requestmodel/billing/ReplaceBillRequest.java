package com.badminton.requestmodel.billing;

import com.badminton.model.billing.BuyerInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thay thế: a full replacement of the original bill. The original is voided
 * and the replacement carries the corrected totals.
 */
@Data
public class ReplaceBillRequest {
    @NotBlank(message = "Vui lòng nhập lý do thay thế.")
    private String reason;

    /** Full corrected line set; empty → the original lines are copied verbatim. */
    private List<AdjustItemRequest> items;

    /** Optional corrected buyer block; null → copied from the original. */
    private BuyerInfo buyer;

    /** Optional per-bill VAT % override — ADMINISTRATOR/ROOT only. */
    private BigDecimal vatRate;
}
