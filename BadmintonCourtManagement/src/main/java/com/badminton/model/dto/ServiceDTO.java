package com.badminton.model.dto;

import com.badminton.entity.Service;
import com.badminton.requestmodel.ResponseDTO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ServiceDTO extends ResponseDTO {
    private String serviceName;
    private float cost;

    /**
     * Stock item this service/good draws from; null for pure pricing lines
     * (time-based services, advance payment, debit adjustments).
     */
    private Integer itemId;

    /**
     * Units sold; null/absent means 1 (backward compatible with existing JSON).
     */
    private Integer quantity;

    public ServiceDTO(String serviceName, float cost) {
        this.serviceName = serviceName;
        this.cost = cost;
    }

    public ServiceDTO(Service serviceEntity) {
        this.serviceName = serviceEntity.getSerName();
        this.cost = serviceEntity.getCost();
        this.itemId = serviceEntity.getItem() != null ? serviceEntity.getItem().getItemId() : null;
    }
}
