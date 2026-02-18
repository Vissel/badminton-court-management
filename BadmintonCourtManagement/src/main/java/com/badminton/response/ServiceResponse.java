package com.badminton.response;

import com.badminton.entity.Service;
import com.badminton.requestmodel.ResponseDTO;
import com.badminton.util.MoneyUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServiceResponse extends ResponseDTO {
    private String serviceName;
    private float cost;
    private String costFormat;
    private final String currency = MoneyUtils.CURRENCY_VN;

    public ServiceResponse(Service serviceEntity) {
        this.serviceName = serviceEntity.getSerName();
        this.cost = serviceEntity.getCost();
        this.costFormat = MoneyUtils.formatToVND(serviceEntity.getCost());
    }
}
