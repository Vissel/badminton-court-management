package com.badminton.service;

import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import com.badminton.response.AppUserResponse;
import com.badminton.response.result.Result;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {

    boolean saveAdminUser(RegisterUserDTO userDTO);

    Result<List<AppUserResponse>> listUsers();

    Result<AppUserResponse> updateUserRole(Long userId, String role);

    Result<AppUserResponse> updateUserStatus(Long userId, boolean active);

    boolean savePlayer(RegisterUserDTO userDTO);

    ResponseEntity<String> generateResetPassToken(String userName);

    ResponseEntity<String> resetPassword(ResetUserRequest resetUserRequest);

    boolean checkUserExistByName(String username);
}
