package com.badminton.response;

import com.badminton.response.result.ShuttleBallResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetUpServiceResponse {
    private int totalCourt;
    private String costInPerson;
    private String rentByTime;
    private List<ShuttleBallResponse> shuttleBalls = new ArrayList<>();
    private List<ServiceResponse> services = new ArrayList<>();
}
