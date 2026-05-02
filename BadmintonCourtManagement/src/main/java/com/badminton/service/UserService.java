package com.badminton.service;

import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import org.springframework.http.ResponseEntity;

public interface UserService {

    boolean saveAdminUser(RegisterUserDTO userDTO);

    boolean savePlayer(RegisterUserDTO userDTO);

    ResponseEntity<String> generateResetPassToken(String userName);

    ResponseEntity<String> resetPassword(ResetUserRequest resetUserRequest);
}
