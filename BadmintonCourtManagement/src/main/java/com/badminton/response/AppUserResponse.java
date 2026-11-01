package com.badminton.response;

import com.badminton.entity.AppUser;
import com.badminton.enums.RoleName;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AppUserResponse {
    private Long userId;
    private String username;
    private String displayName;
    private List<String> roles;
    private Map<String, String> vnRoleMapping;
    private boolean active;
    /** Manager (administrator) of this coordinator; null when unassigned. */
    private Long managerUserId;
    private String managerUsername;

    /** Vietnamese display label for each PascalCase role key. */
    private static final Map<String, String> VN_ROLE_LABELS = buildVnRoleLabels();

    private static Map<String, String> buildVnRoleLabels() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Root", "Quản trị gốc");
        m.put("Administrator", "Quản trị viên");
        m.put("Coordinator", "Điều phối viên");
        m.put("Player", "Người chơi");
        return m;
    }

    private static String toPascalCase(RoleName role) {
        if (role == null)
            return null;
        String name = role.name().toLowerCase();
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public static AppUserResponse from(AppUser user) {
        AppUserResponse response = new AppUserResponse();
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        List<String> pascalRoles = user.getRoles().stream()
                .map(role -> toPascalCase(role.getRoleName()))
                .sorted()
                .toList();
        response.setRoles(pascalRoles);
        response.setVnRoleMapping(VN_ROLE_LABELS);
        response.setActive(user.isActive());
        return response;
    }
}
