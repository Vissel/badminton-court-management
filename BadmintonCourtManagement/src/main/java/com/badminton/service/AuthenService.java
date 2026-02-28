package com.badminton.service;

import com.badminton.integration.AuthenClient;
import com.badminton.response.AuthenBody;
import com.badminton.response.AuthenResponse;
import com.badminton.response.result.Result;
import com.qrpublic.apartment.authentication.interfaces.request.NormalLoginRequest;
import com.qrpublic.apartment.authentication.interfaces.response.NormalLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service("authenService")
@RequiredArgsConstructor
public class AuthenService {
    private final AuthenClient authenClient;

    /**
     * get public key
     *
     * @return
     */
    public Result<ByteArrayResource> getPublicKey() {
        ResponseEntity<ByteArrayResource> byteResponse = authenClient.getPublicKey();
        return convertToByteResult(byteResponse);
    }

    private Result<ByteArrayResource> convertToByteResult(ResponseEntity<ByteArrayResource> byteResponse) {
        Result result = new Result();
        result.setErrorCode(byteResponse.getStatusCode().value());
        if (byteResponse.getStatusCode().is2xxSuccessful()) {
            result.setSuccess(true);
            result.setData(byteResponse.getBody());
        } else {
            log.error("Unable to get public key");
            result.setData(new ByteArrayResource("Unable to get public key".getBytes()));
        }
        return result;
    }

    /**
     * login with secure
     *
     * @param username
     * @param encryptedPassword
     * @return
     */
    public AuthenResponse secureLogin(String username, String encryptedPassword) {
        NormalLoginRequest intRequest = new NormalLoginRequest();
        intRequest.setUsername(username);
        intRequest.setEncryptedPassword(encryptedPassword);
        ResponseEntity<com.qrpublic.apartment.template.model.Result<NormalLoginResponse>> responseEntity = authenClient.normalLogin(intRequest);

        return convertToAuthenResponse(responseEntity);
    }

    private AuthenResponse convertToAuthenResponse(ResponseEntity<com.qrpublic.apartment.template.model.Result<NormalLoginResponse>> responseEntity) {
        AuthenResponse authenResponse = new AuthenResponse();
        authenResponse.setStatus(responseEntity.getStatusCode().value());
//        authenResponse.setBody(convertToAuthenBody(responseEntity.getBody()));
        NormalLoginResponse data = responseEntity.getBody().getData();
        authenResponse.setUsername(data.getUsername());
        authenResponse.setToken(data.getToken());
        authenResponse.setMessage(data.getMessage());
        return authenResponse;
    }

    private AuthenBody convertToAuthenBody(com.qrpublic.apartment.template.model.Result<NormalLoginResponse> intNormalLoginResponse) {
        AuthenBody body = new AuthenBody();
        NormalLoginResponse data = intNormalLoginResponse.getData();
        body.setValid(data.getAuthenticated());
        return body;
    }
}
