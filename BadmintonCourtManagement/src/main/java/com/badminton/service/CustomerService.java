package com.badminton.service;

import com.badminton.requestmodel.billing.CustomerRequest;
import com.badminton.response.billing.CustomerResponse;
import com.badminton.response.result.Result;

import java.util.List;

/**
 * Buyer directory (G-9). Optional data used to speed up checkout for regulars
 * and B2B customers; it never blocks a bill from being issued.
 */
public interface CustomerService {

    /** Autocomplete for the checkout buyer block. */
    Result<List<CustomerResponse>> search(String keyword);

    Result<CustomerResponse> save(CustomerRequest request);

    Result<Void> delete(Long customerId);
}
