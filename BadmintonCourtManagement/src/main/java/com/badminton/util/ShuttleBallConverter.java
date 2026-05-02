package com.badminton.util;

import com.badminton.model.dto.ShuttleBallDTO;
import com.badminton.requestmodel.ShuttleBallRequest;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ShuttleBallConverter {
    public static List<ShuttleBallDTO> convertRequestToDTO(List<ShuttleBallRequest> listBallRequest) {
        return listBallRequest.stream()
                .filter(Objects::nonNull)
                .map(req ->
                        new ShuttleBallDTO(req.getShuttleName(), Float.valueOf(req.getCost()), req.getQuantity(),
                                false)).collect(Collectors.toList());
    }
}
