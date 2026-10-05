package com.badminton.response.billing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BillItemResponse {
    private int lineNo;
    private String itemType;
    private String itemName;
    private BigDecimal qty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String note;
}
