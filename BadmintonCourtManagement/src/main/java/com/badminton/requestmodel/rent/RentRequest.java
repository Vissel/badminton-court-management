package com.badminton.requestmodel.rent;

import com.badminton.model.dto.ShuttleBallDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public abstract class RentRequest {
    @NotNull
    @NotBlank
    private String courtId;

    @NotNull
    @NotBlank
    private String playerName;

    public abstract List<ShuttleBallDTO> getShuttleBalls();
}
