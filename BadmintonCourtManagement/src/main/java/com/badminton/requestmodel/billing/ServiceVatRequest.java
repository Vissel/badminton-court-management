package com.badminton.requestmodel.billing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Per-service VAT rate edited from Setup. Null clears the override so the
 * venue-wide {@code bill_config.vat_rate} applies again.
 */
@Data
public class ServiceVatRequest {
    private BigDecimal vatRate;
}
