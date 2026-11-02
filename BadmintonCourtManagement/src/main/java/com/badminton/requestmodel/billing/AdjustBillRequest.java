package com.badminton.requestmodel.billing;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Điều chỉnh: a bill carrying only the difference against the original.
 * Positive lines increase the original, negative lines decrease it.
 */
@Data
public class AdjustBillRequest {
    @NotBlank(message = "Vui lòng nhập lý do điều chỉnh.")
    private String reason;

    /** Delta lines. Empty → the service rejects the request. */
    private List<AdjustItemRequest> items;

    /** Optional per-bill VAT % override — ADMINISTRATOR/ROOT only. */
    private BigDecimal vatRate;
}
