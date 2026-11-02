package com.badminton.response.billing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * VAT override of one service as shown on Setup. A null rate means the
 * venue-wide {@code bill_config.vat_rate} applies.
 */
@Data
public class ServiceVatResponse {
    private Long serviceId;
    private String serviceName;
    private BigDecimal vatRate;
}
