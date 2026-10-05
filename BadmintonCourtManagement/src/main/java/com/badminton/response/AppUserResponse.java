package com.badminton.response;

import com.badminton.entity.AppUser;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AppUserResponse {
    private Long userId;
    private String username;
    private String displayName;
    private List<String> roles;
    private boolean active;

    public static AppUserResponse from(AppUser user) {
        AppUserResponse response = new AppUserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setRoles(user.getRoles().stream()
                .map(role -> role.getRoleName().name()).sorted().toList());
        response.setActive(user.isActive());
        return response;
    }
}
