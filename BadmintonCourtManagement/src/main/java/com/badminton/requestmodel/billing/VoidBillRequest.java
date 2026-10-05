package com.badminton.requestmodel.billing;

import lombok.Data;

@Data
public class VoidBillRequest {
    private String reason;
}
