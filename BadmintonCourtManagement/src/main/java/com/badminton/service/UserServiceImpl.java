package com.badminton.service;

import com.badminton.config.cache.AppCache;
import com.badminton.entity.AppUser;
import com.badminton.entity.Player;
import com.badminton.entity.Role;
import com.badminton.enums.RoleName;
import com.badminton.model.CacheObject;
import com.badminton.repository.AppUserRepository;
import com.badminton.repository.RoleRepository;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import com.badminton.response.AppUserResponse;
import com.badminton.response.result.Result;
import com.badminton.util.RsaKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    // Rank order enforces the management hierarchy: an actor may only manage
    // accounts whose highest role is strictly below the actor's own rank.
    private static final int ROOT_RANK = 3;
    private static final int ADMIN_RANK = 2;
    private static final int COORDINATOR_RANK = 1;

    private final PasswordEncoder encoder;
    private final UserRepository playerRepository;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final AppCache appCache;
    private final RsaKeyService rsaKeyService;

    public UserServiceImpl(PasswordEncoder encoder, UserRepository playerRepository,
            AppUserRepository appUserRepository, RoleRepository roleRepository,
            AppCache appCache, RsaKeyService rsaKeyService) {
        this.encoder = encoder;
        this.playerRepository = playerRepository;
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.appCache = appCache;
        this.rsaKeyService = rsaKeyService;
    }

    @Override
    public boolean saveAdminUser(RegisterUserDTO userDTO) {
        if (userDTO.getUserId() != null || appUserRepository.existsByUsername(userDTO.getUserName()))
            return false;
        try {
            RoleName roleName = userDTO.getRole() == null || userDTO.getRole().isBlank()
                    ? RoleName.ADMINISTRATOR
                    : RoleName.valueOf(userDTO.getRole().trim().toUpperCase());
            if (roleName == RoleName.PLAYER || !canManageRank(rankOf(roleName)))
                return false;
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role is not configured"));
            String rawPassword = rsaKeyService.decrypt(userDTO.getPassword());
            return appUserRepository.save(new AppUser(userDTO.getUserName(), encoder.encode(rawPassword), role))
                    .getUserId() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean savePlayer(RegisterUserDTO userDTO) {
        return playerRepository.save(new Player(userDTO.getUserName(), null)).getPlayerId() != 0;
    }

    @Override
    public ResponseEntity<String> generateResetPassToken(String userName) {
        AppUser user = appUserRepository.findByUsername(userName).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User is not present.");
        if (!canManageRank(rankOf(user)))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient permission to manage this user.");
        String token = encoder.encode(userName + UUID.randomUUID() + System.currentTimeMillis());
        appCache.put(token, new CacheObject(userName, System.currentTimeMillis()));
        return ResponseEntity.ok(token);
    }

    @Override
    public ResponseEntity<String> resetPassword(ResetUserRequest request) {
        try {
            request.setNewPass(rsaKeyService.decrypt(request.getNewPass()));
            request.setRepeatNewPass(rsaKeyService.decrypt(request.getRepeatNewPass()));
            validateRequest(request);
            AppUser user = appUserRepository.findByUsername(request.getUserName())
                    .orElseThrow(() -> new IllegalArgumentException("User is not present."));
            Assert.isTrue(canManageRank(rankOf(user)), "Insufficient permission to manage this user.");
            user.setPassword(encoder.encode(request.getNewPass()));
            appUserRepository.save(user);
            appCache.remove(request.getResetToken());
            return ResponseEntity.ok("Reset password for user " + request.getUserName() + " successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private void validateRequest(ResetUserRequest request) {
        Assert.isTrue(appCache.contains(request.getResetToken()), "Token is invalid");
        CacheObject cacheObject = (CacheObject) appCache.get(request.getResetToken());
        Assert.notNull(cacheObject, "Token is invalid");
        Assert.isTrue(System.currentTimeMillis() - cacheObject.getExpiryTime() < 1000 * 60 * 3, "Token is expired.");
        Assert.isTrue(request.getUserName().equals(cacheObject.getValue()), "User is not present.");
        Assert.isTrue(request.getNewPass().equals(request.getRepeatNewPass()), "Two passwords must match.");
    }

    @Override
    public Result<List<AppUserResponse>> listUsers() {
        Result<List<AppUserResponse>> result = new Result<>();
        result.setSuccess(true);
        result.setData(appUserRepository.findAll().stream().map(AppUserResponse::from).toList());
        return result;
    }

    @Override
    public Result<AppUserResponse> updateUserRole(Long userId, String roleName) {
        AppUser user = findManageableUser(userId);
        if (user == null)
            return forbidden();
        if (user.getUsername().equals(actorName()))
            return badRequest("Cannot change your own role.");
        RoleName targetRole;
        try {
            targetRole = RoleName.valueOf(roleName.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return badRequest("Invalid role: " + roleName);
        }
        if (targetRole == RoleName.PLAYER || !canManageRank(rankOf(targetRole)))
            return forbidden();
        user.getRoles().clear();
        user.getRoles().add(roleRepository.findByRoleName(targetRole)
                .orElseThrow(() -> new IllegalStateException("Role is not configured")));
        Result<AppUserResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(AppUserResponse.from(appUserRepository.save(user)));
        return result;
    }

    @Override
    public Result<AppUserResponse> updateUserStatus(Long userId, boolean active) {
        AppUser user = findManageableUser(userId);
        if (user == null)
            return forbidden();
        if (user.getUsername().equals(actorName()))
            return badRequest("Cannot deactivate your own account.");
        user.setActive(active);
        Result<AppUserResponse> result = new Result<>();
        result.setSuccess(true);
        result.setData(AppUserResponse.from(appUserRepository.save(user)));
        return result;
    }

    @Override
    public boolean checkUserExistByName(String username) {
        return appUserRepository.existsByUsername(username);
    }

    /**
     * Returns the target user only when the current actor is allowed to manage
     * it: ROOT manages everyone, ADMINISTRATOR manages only COORDINATOR-level
     * accounts. PLAYER accounts are never manageable targets.
     */
    private AppUser findManageableUser(Long userId) {
        AppUser user = appUserRepository.findById(userId).orElse(null);
        if (user == null || !canManageRank(rankOf(user)))
            return null;
        return user;
    }

    private boolean canManageRank(int targetRank) {
        int actorRank = actorRank();
        return actorRank == ROOT_RANK || targetRank < actorRank;
    }

    private int rankOf(AppUser user) {
        return user.getRoles().stream().mapToInt(role -> rankOf(role.getRoleName())).max().orElse(0);
    }

    private int rankOf(RoleName role) {
        return switch (role) {
            case ROOT -> ROOT_RANK;
            case ADMINISTRATOR -> ADMIN_RANK;
            case COORDINATOR -> COORDINATOR_RANK;
            case PLAYER -> 0;
        };
    }

    private int actorRank() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return 0;
        if (hasAuthority(authentication, "ROLE_ROOT"))
            return ROOT_RANK;
        if (hasAuthority(authentication, "ROLE_ADMINISTRATOR"))
            return ADMIN_RANK;
        if (hasAuthority(authentication, "ROLE_COORDINATOR"))
            return COORDINATOR_RANK;
        return 0;
    }

    private String actorName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "" : authentication.getName();
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    private <T> Result<T> forbidden() {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.FORBIDDEN.value());
        result.setErrorMessage("Insufficient permission to manage this user.");
        return result;
    }

    private <T> Result<T> badRequest(String message) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setErrorCode(HttpStatus.BAD_REQUEST.value());
        result.setErrorMessage(message);
        return result;
    }
}
