package com.badminton.integration;

import com.qrpublic.apartment.template.model.Result;
import com.qrpublic.apartment.user.interfaces.request.UserCreateRequest;
import com.qrpublic.apartment.user.interfaces.response.UserCreateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PublicLinkApplication", url = "https://localhost:8080/publiclink/api/v1/user")
public interface UserClient {

    @PostMapping("/createUser")
    ResponseEntity<Result<UserCreateResponse>> createUser(@RequestBody UserCreateRequest request);
}
