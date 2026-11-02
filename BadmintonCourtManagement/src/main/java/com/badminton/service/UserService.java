package com.badminton.service;

import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import com.badminton.response.AppUserResponse;
import com.badminton.response.UserDeleteBatchResponse;
import com.badminton.response.result.Result;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService {

    boolean saveAdminUser(RegisterUserDTO userDTO);

    Result<List<AppUserResponse>> listUsers();

    Result<AppUserResponse> updateUserRole(Long userId, String role);

    Result<AppUserResponse> updateUserStatus(Long userId, boolean active);

    /**
     * ROOT-only: soft-deletes an account so it disappears from user management
     * and can no longer log in.
     */
    Result<AppUserResponse> deleteUser(Long userId);

    /**
     * ROOT-only: soft-deletes several selected accounts in one go. Accounts that
     * cannot be removed are reported in the response instead of aborting the
     * whole request.
     */
    Result<UserDeleteBatchResponse> deleteUsers(List<Long> userIds);

    /**
     * ROOT-only: assigns a coordinator to an administrator (null clears the link).
     */
    Result<AppUserResponse> assignManager(Long userId, Long managerUserId);

    boolean savePlayer(RegisterUserDTO userDTO);

    ResponseEntity<String> generateResetPassToken(String userName);

    ResponseEntity<String> resetPassword(ResetUserRequest resetUserRequest);

    boolean checkUserExistByName(String username);
}
