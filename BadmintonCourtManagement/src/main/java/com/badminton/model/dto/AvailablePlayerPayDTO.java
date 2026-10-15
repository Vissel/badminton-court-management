package com.badminton.model.dto;

import com.badminton.constant.PayType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class AvailablePlayerPayDTO {
    private String name;
    private List<ServiceDTO> listService;
    private PayType payType;
    private float payAmount;
}
