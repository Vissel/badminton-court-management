package com.badminton.response.billing;

import com.badminton.entity.Customer;
import lombok.Data;

/**
 * Buyer directory entry returned to the checkout autocomplete.
 */
@Data
public class CustomerResponse {
    private Long customerId;
    private String name;
    private String company;
    private String taxCode;
    private String address;
    private String phone;
    private String email;
    private String bankName;
    private String bankAccount;
    private boolean active;

    public static CustomerResponse from(Customer customer) {
        CustomerResponse res = new CustomerResponse();
        res.setCustomerId(customer.getCustomerId());
        res.setName(customer.getName());
        res.setCompany(customer.getCompany());
        res.setTaxCode(customer.getTaxCode());
        res.setAddress(customer.getAddress());
        res.setPhone(customer.getPhone());
        res.setEmail(customer.getEmail());
        res.setBankName(customer.getBankName());
        res.setBankAccount(customer.getBankAccount());
        res.setActive(customer.isActive());
        return res;
    }
}
