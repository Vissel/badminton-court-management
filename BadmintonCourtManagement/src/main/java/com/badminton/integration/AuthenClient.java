package com.badminton.integration;

import com.qrpublic.apartment.authentication.interfaces.request.NormalLoginRequest;
import com.qrpublic.apartment.authentication.interfaces.response.NormalLoginResponse;
import com.qrpublic.apartment.template.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "PublicLinkApplication", url = "https://localhost:8080/publiclink/api/v1/server-auth")
public interface AuthenClient {

    @PostMapping("/normal")
    ResponseEntity<Result<NormalLoginResponse>> normalLogin(@RequestBody NormalLoginRequest request);

    @GetMapping("/public-key")
    ResponseEntity<ByteArrayResource> getPublicKey();
}
