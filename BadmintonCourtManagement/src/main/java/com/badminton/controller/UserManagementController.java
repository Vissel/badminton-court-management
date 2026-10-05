package com.badminton.controller;

import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import com.badminton.requestmodel.UpdateUserRequest;
import com.badminton.response.AppUserResponse;
import com.badminton.response.result.Result;
import com.badminton.service.UserService;
import com.badminton.util.ResponseConvertor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserManagementController {
    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Result<List<AppUserResponse>>> listUsers() {
        return ResponseConvertor.convert(userService.listUsers());
    }

    @PostMapping
    public ResponseEntity<Result<AppUserResponse>> createUser(@RequestBody RegisterUserDTO userDTO) {
        return ResponseConvertor.convert(booleanResult(userService.saveAdminUser(userDTO), userDTO.getUserName()));
    }

    @PutMapping("/{userId}/role")
    public ResponseEntity<Result<AppUserResponse>> updateUserRole(@PathVariable Long userId,
                                                                  @RequestBody UpdateUserRequest request) {
        return ResponseConvertor.convert(userService.updateUserRole(userId, request.getRole()));
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<Result<AppUserResponse>> updateUserStatus(@PathVariable Long userId,
                                                                    @RequestBody UpdateUserRequest request) {
        if (request.getActive() == null) return badRequest("active is required.");
        return ResponseConvertor.convert(userService.updateUserStatus(userId, request.getActive()));
    }

    @GetMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestParam("username") String username) {
        return userService.generateResetPassToken(username);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetUserRequest request) {
        return userService.resetPassword(request);
    }

    private Result<AppUserResponse> booleanResult(boolean saved, String username) {
        Result<AppUserResponse> result = new Result<>();
        if (saved) {
            result.setSuccess(true);
            AppUserResponse response = new AppUserResponse();
            response.setUsername(username);
            result.setData(response);
        } else {
            result.setSuccess(false);
            result.setErrorCode(HttpStatus.FORBIDDEN.value());
            result.setErrorMessage("Could not create user.");
        }
        return result;
    }

    private <T> ResponseEntity<Result<T>> badRequest(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.BAD_REQUEST.value());
        result.setErrorMessage(message);
        return ResponseEntity.badRequest().body(result);
    }
}
