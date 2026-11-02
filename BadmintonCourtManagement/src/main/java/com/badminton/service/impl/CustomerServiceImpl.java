package com.badminton.service.impl;

import com.badminton.entity.Customer;
import com.badminton.repository.CustomerRepository;
import com.badminton.requestmodel.billing.CustomerRequest;
import com.badminton.response.billing.CustomerResponse;
import com.badminton.response.result.Result;
import com.badminton.service.CustomerService;
import com.badminton.service.ProcessCallback;
import com.badminton.service.ServiceTemplate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final int SEARCH_LIMIT = 20;

    @Autowired
    private ServiceTemplate serviceTemple;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public Result<List<CustomerResponse>> search(String keyword) {
        return serviceTemple.execute(new ProcessCallback<String, List<CustomerResponse>>() {
            @Override
            public String getRequest() {
                return keyword;
            }

            @Override
            public void preProcess(String request) {
            }

            @Override
            public List<CustomerResponse> process() {
                String trimmed = StringUtils.isNotBlank(getRequest()) ? getRequest().trim() : null;
                return customerRepository.search(trimmed, PageRequest.of(0, SEARCH_LIMIT)).stream()
                        .map(CustomerResponse::from)
                        .collect(Collectors.toList());
            }
        });
    }

    @Override
    public Result<CustomerResponse> save(CustomerRequest request) {
        return serviceTemple.execute(new ProcessCallback<CustomerRequest, CustomerResponse>() {
            @Override
            public CustomerRequest getRequest() {
                return request;
            }

            @Override
            public void preProcess(CustomerRequest request) {
                Assert.notNull(request, "Request must not be null");
                Assert.isTrue(StringUtils.isNotBlank(request.getName()),
                        "Tên khách hàng không được để trống");
            }

            @Override
            public CustomerResponse process() {
                CustomerRequest req = getRequest();
                Customer customer = req.getCustomerId() != null
                        ? customerRepository.findById(req.getCustomerId())
                                .orElseGet(Customer::new)
                        : new Customer();
                customer.setName(req.getName().trim());
                customer.setCompany(req.getCompany());
                customer.setTaxCode(req.getTaxCode());
                customer.setAddress(req.getAddress());
                customer.setPhone(req.getPhone());
                customer.setEmail(req.getEmail());
                customer.setBankName(req.getBankName());
                customer.setBankAccount(req.getBankAccount());
                if (req.getActive() != null) {
                    customer.setActive(req.getActive());
                }
                return CustomerResponse.from(customerRepository.save(customer));
            }
        });
    }

    @Override
    public Result<Void> delete(Long customerId) {
        return serviceTemple.execute(new ProcessCallback<Long, Void>() {
            @Override
            public Long getRequest() {
                return customerId;
            }

            @Override
            public void preProcess(Long request) {
                Assert.notNull(request, "Customer id must not be null");
            }

            @Override
            public Void process() {
                // Soft delete: bills already issued keep pointing at the name
                // text, but the directory stops offering the entry.
                customerRepository.findById(getRequest()).ifPresent(customer -> {
                    customer.setActive(false);
                    customerRepository.save(customer);
                });
                return null;
            }
        });
    }
}
