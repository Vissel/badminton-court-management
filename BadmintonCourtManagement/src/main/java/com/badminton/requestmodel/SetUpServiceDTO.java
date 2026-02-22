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
public class SetUpServiceDTO {
    private int totalCourt;
    private float costInPerson;
    private List<ShuttleBallDTO> shuttleBalls;
    private List<ServiceDTO> services;
}
