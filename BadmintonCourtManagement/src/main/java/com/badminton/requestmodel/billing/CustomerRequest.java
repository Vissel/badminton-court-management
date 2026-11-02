package com.badminton.requestmodel.billing;

import lombok.Data;

/**
 * Buyer directory entry (P3 convenience for regulars/B2B customers).
 */
@Data
public class CustomerRequest {
    private Long customerId;
    private String name;
    private String company;
    private String taxCode;
    private String address;
    private String phone;
    private String email;
    private String bankName;
    private String bankAccount;
    private Boolean active;
}
