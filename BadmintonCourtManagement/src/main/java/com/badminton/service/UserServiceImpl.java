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
import com.badminton.util.RsaKeyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
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
            if (roleName == RoleName.PLAYER)
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
        if (!checkUserExistByName(userName))
            return ResponseEntity.badRequest().body("User is not present.");
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
    public boolean checkUserExistByName(String username) {
        return appUserRepository.existsByUsername(username);
    }
}
