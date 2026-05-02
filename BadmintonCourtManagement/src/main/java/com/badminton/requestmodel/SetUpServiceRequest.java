package com.badminton.requestmodel;

import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.dto.ShuttleBallDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetUpServiceRequest {
    private int totalCourt;
    private float costInPerson;
    private List<ShuttleBallDTO> addedShuttleBalls;
    private List<ShuttleBallDTO> deletedShuttleBalls;
    private List<ServiceDTO> addedServices;
    private List<ServiceDTO> deletedServices;
}
