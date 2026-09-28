package com.badminton.model.report;

import com.badminton.constant.PayType;
import com.badminton.entity.Player;
import com.badminton.entity.Session;
import com.badminton.model.dto.ServiceDTO;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class PlayerRptModel {
    private String playerName;

    private int sessionId;

    private Instant leaveTime;

    private List<ServiceDTO> serviceDTOs;

    private BigDecimal payAmount;

    private PayType payType;

    private BigDecimal advancePay;
}
