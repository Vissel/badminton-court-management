package com.badminton.requestmodel;

import lombok.Data;

@Data
public class ResetUserRequest {
    private String userName;
    private String newPass;
    private String repeatNewPass;
    private String resetToken;
}
