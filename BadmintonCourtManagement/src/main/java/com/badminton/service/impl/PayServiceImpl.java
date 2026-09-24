package com.badminton.service.impl;

import com.badminton.core.payment.CorePaymentService;
import com.badminton.exception.BusinessException;
import com.badminton.model.dto.CreateDebitDTO;
import com.badminton.model.dto.PaymentDTO;
import com.badminton.model.dto.ServiceDTO;
import com.badminton.model.payment.PaymentModel;
import com.badminton.requestmodel.PayRequest;
import com.badminton.response.PayResponse;
import com.badminton.response.result.Result;
import com.badminton.service.PayService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import com.badminton.util.ServiceConverter;
import com.badminton.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayServiceImpl implements PayService {
    @Autowired
    ServiceTemplate serviceTemple;
    @Autowired
    CorePaymentService corePaymentService;

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
                Assert.notNull(request.getDebitRequest(), "Debit request must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getDebitRequest().getCreatedTime()), "Debit created time must not be blank");
            }

            @Override
            public PayResponse process() throws BusinessException {
                PaymentDTO paymentDTO = convertToPaymentDTO(getRequest());
                PaymentModel paymentModel = corePaymentService.payForPlayerAndCreateDebt(paymentDTO);
                return convertToPayResponse(paymentModel);
            }
        });
    }

    private PaymentDTO convertToPaymentDTO(PayRequest request) {
        PaymentDTO dto = new PaymentDTO();
        dto.setPlayerName(request.getPlayerName());
        dto.setServices(request.getServiceRequests() != null
                ? request.getServiceRequests().stream()
                        .map(ServiceConverter::convertRequestToDTO)
                        .collect(Collectors.toList())
                : null);
        dto.setTotalPay(request.getTotalExpense());
        dto.setPayType(request.getPayType());
        dto.setDebit(request.getDebitRequest() != null ? convertToCreateDebitDTO(request.getDebitRequest()) : null);
        return dto;
    }

    private CreateDebitDTO convertToCreateDebitDTO(com.badminton.requestmodel.debit.DebitRequest request) {
        CreateDebitDTO dto = new CreateDebitDTO();
        dto.setDebitAmount(BigDecimal.valueOf(request.getDebitAmount()));
        dto.setCurrency(request.getCurrency());
        dto.setNote(request.getNote());
        dto.setPlayerName(request.getPlayerName());
        dto.setCreatedTime(TimeUtils.convertToInstant(request.getCreatedTime()));
        return dto;
    }

    private PayResponse convertToPayResponse(PaymentModel paymentModel) {
        return new PayResponse(
                paymentModel.getPayFor(),
                paymentModel.getServices(),
                paymentModel.getPayType(),
                paymentModel.getPayAmount() != null ? paymentModel.getPayAmount().floatValue() : 0f,
                paymentModel.getPayTime() != null ? paymentModel.getPayTime().toString() : null,
                paymentModel.getDebitAmount() != null ? paymentModel.getDebitAmount().floatValue() : 0f);
    }
}
