package com.badminton.response;

import com.badminton.entity.Service;
import com.badminton.model.dto.ServiceDTO;
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

    /** Linked stock item id; null for pure pricing rows. */
    private Integer itemId;

    /** Current stock in base units; null when this is not a stockable item. */
    private Long stockOnHand;

    /** True when stockOnHand is non-null and <= 10. */
    private Boolean lowStock;

    public ServiceResponse(Service serviceEntity) {
        this.serviceName = serviceEntity.getSerName();
        this.cost = serviceEntity.getCost();
        this.costFormat = MoneyUtils.formatToVND(serviceEntity.getCost());
        this.itemId = serviceEntity.getItem() != null ? serviceEntity.getItem().getItemId() : null;
    }

    public ServiceResponse(ServiceDTO serviceDTO) {
        this.serviceName = serviceDTO.getServiceName();
        this.cost = serviceDTO.getCost();
        this.costFormat = MoneyUtils.formatToVND(serviceDTO.getCost());
        this.itemId = serviceDTO.getItemId();
    }

}
