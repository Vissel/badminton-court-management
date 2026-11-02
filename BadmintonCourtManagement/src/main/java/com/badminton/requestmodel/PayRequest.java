package com.badminton.requestmodel;

import com.badminton.constant.PayType;
import com.badminton.model.billing.BuyerInfo;
import com.badminton.requestmodel.debit.DebitRequest;
import com.badminton.requestmodel.debit.PayDebitRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PayRequest {
    @NotBlank
    private String playerName;
    private List<ServiceRequest> serviceRequests;
    private String totalExpense;
    private PayType payType;
    private DebitRequest debitRequest;
    private PayDebitRequest payDebits;
    /**
     * Optional buyer tax info captured at checkout (business customers).
     */
    private BuyerInfo buyer;
    /**
     * Optional per-bill VAT % override — ignored unless the caller is
     * ADMINISTRATOR or ROOT.
     */
    private BigDecimal vatRate;
    /** Hint for the FE: auto-open the receipt print view after payment. */
    private Boolean printBill;
    /**
     * Optional invoice-level discount (VND, VAT-inclusive). Recorded as a
     * negative DISCOUNT line so the printed total matches what was collected.
     */
    private BigDecimal discountAmount;
    /**
     * Idempotency key of one checkout attempt: a retried request carrying the
     * same value returns the bill already issued by the first attempt.
     */
    private String clientTransactionId;
}
