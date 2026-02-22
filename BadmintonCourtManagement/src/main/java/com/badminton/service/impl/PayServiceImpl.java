package com.badminton.service.impl;

import com.badminton.entity.AvailablePlayer;
import com.badminton.exception.BusinessException;
import com.badminton.exception.enums.ErrorCodeEnum;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.repository.AvailablePlayerRepository;
import com.badminton.requestmodel.PayRequest;
import com.badminton.response.PayResponse;
import com.badminton.response.result.Result;
import com.badminton.service.PayService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemple;
import com.badminton.service.SessionServiceImpl;
import com.badminton.util.ServiceConverter;
import com.badminton.util.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PayServiceImpl implements PayService {
    @Autowired
    ServiceTemple serviceTemple;
    @Autowired
    AvailablePlayerRepository availablePlayerRepository;
    @Autowired
    SessionServiceImpl sessionService;

    @Transactional
    @Override
    public Result<PayResponse> payToPlayer(PayRequest payRequest) {
        return serviceTemple.execute(new ProcessCallback<PayRequest, PayResponse>() {
            @Override
            public PayRequest getRequest() {
                return payRequest;
            }

            @Override
            public void preProcess(PayRequest request) {
                Assert.notNull(request, "Request must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getPlayerName()), "playerName must not be blank");
                Assert.notNull(request.getServiceRequests(), "Service must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getPayType()), "Pay type  must not be blank");
            }

            @Override
            public PayResponse process() throws BusinessException {
                Optional<AvailablePlayer> optPlayer = availablePlayerRepository.findAvailablePlayerInSessionByName(
                        sessionService.findListCurrentSession().getFirst(), payRequest.getPlayerName());
                if (!optPlayer.isPresent()) {
                    throw new BusinessException(ErrorCodeEnum.PLAYER_NOT_FOUND, "Available player is not found.");
                }
                AvailablePlayer availablePlayer = optPlayer.get();
                List<ServiceDTO> dtos = payRequest.getServiceRequests().stream()
                        .map(req -> ServiceConverter.convertRequestToDTO(req))
                        .collect(Collectors.toList());
                availablePlayer.setServices(
                        ServiceUtil.buildJsonArrayStr(dtos));
                availablePlayer.setLeaveTime(ServiceUtil.getCurrentInstant());
                availablePlayer.setPayType(payRequest.getPayType());
                availablePlayer.setPayAmount(Float.valueOf(payRequest.getTotalExpense()));
                return convertToPayResult(availablePlayerRepository.save(availablePlayer));
            }
        });
    }


    private PayResponse convertToPayResult(AvailablePlayer availablePlayer) {
        return new PayResponse(availablePlayer.getPlayer().getPlayerName(), availablePlayer.getCurrentServices(),
                availablePlayer.getPayType(), availablePlayer.getPayAmount(), availablePlayer.getLeaveTime().toString()
        );
    }
}
