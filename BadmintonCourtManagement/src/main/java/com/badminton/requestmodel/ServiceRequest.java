package com.badminton.requestmodel;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceRequest extends ResponseDTO {
    private String serviceName;
    private String cost;

    /**
     * Stock item id for sellable goods; null for non-stockable services.
     */
    private Integer itemId;

    /**
     * Units sold; null/absent means 1.
     */
    private Integer quantity;

    public ServiceRequest(String serviceName, String cost) {
        this.serviceName = serviceName;
        this.cost = cost;
    }
}
