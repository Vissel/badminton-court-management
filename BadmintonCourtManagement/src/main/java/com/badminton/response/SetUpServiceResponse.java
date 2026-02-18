package com.badminton.response;

import com.badminton.constant.ApiConstant;
import com.badminton.entity.Service;
import com.badminton.entity.ShuttleBall;
import com.badminton.response.result.ShuttleBallResponse;
import com.badminton.util.MoneyUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetUpServiceResponse {
    private int totalCourt;
    private String costInPerson;
    private List<ShuttleBallResponse> shuttleBalls;
    private List<ServiceResponse> services;

    public SetUpServiceResponse(int totalCourt2, List<ShuttleBall> listShuttleBall, List<Service> listService) {
        this.totalCourt = totalCourt2;
        this.shuttleBalls = listShuttleBall.stream()
                .map(ball -> new ShuttleBallResponse(ball)).collect(Collectors.toList());
        this.services = listService.stream().map(ser -> {
            if (ser.getSerName().equals(ApiConstant.COST_IN_PERNSON)) {
                setCostInPerson(MoneyUtils.formatToVND(ser.getCost()));
                return null;
            }
            return new ServiceResponse(ser);

        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
