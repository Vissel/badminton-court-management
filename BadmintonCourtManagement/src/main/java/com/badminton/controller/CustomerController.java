package com.badminton.controller;

import com.badminton.requestmodel.billing.CustomerRequest;
import com.badminton.response.billing.CustomerResponse;
import com.badminton.response.result.Result;
import com.badminton.service.CustomerService;
import com.badminton.util.ResponseConvertor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Buyer directory (G-9): checkout autocomplete plus the admin catalogue used
 * by the Setup page. Writes are restricted to ROOT/ADMINISTRATOR — the FE only
 * hides the controls, which is bypassable.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public ResponseEntity<Result<List<CustomerResponse>>> search(@RequestParam(required = false) String keyword) {
        return ResponseConvertor.convert(customerService.search(keyword));
    }

    @PostMapping
    public ResponseEntity<Result<CustomerResponse>> save(@RequestBody CustomerRequest request) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(customerService.save(request));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Result<Void>> delete(@PathVariable Long customerId) {
        if (!hasAdminRole()) {
            return forbidden();
        }
        return ResponseConvertor.convert(customerService.delete(customerId));
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ROOT")
                        || authority.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }

    private <T> ResponseEntity<Result<T>> forbidden() {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.FORBIDDEN.value());
        result.setErrorMessage("Bạn không có quyền thực hiện thao tác này.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }
}
