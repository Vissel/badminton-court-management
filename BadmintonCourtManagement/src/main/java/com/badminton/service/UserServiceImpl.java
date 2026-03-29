package com.badminton.service;

import com.badminton.config.cache.AppCache;
import com.badminton.entity.Player;
import com.badminton.model.CacheObject;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.RegisterUserDTO;
import com.badminton.requestmodel.ResetUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AppCache appCache;

    @Override
    public boolean saveAdminUser(RegisterUserDTO userDTO) {
        boolean isSaved = false;
        if (userDTO.getUserId() == null) {
            Optional<Player> existedPlayer = userRepo.findByPlayerName(userDTO.getUserName());

            Player user;
            if (!existedPlayer.isPresent()) {
                user = new Player(userDTO.getUserName(), encoder.encode(userDTO.getPassword()));
            } else {
                user = existedPlayer.get();
                user.setPassword(userDTO.getPassword());
            }
            isSaved = userRepo.save(user).getPlayerId() != 0;
        }
        return isSaved;
    }

    @Override
    public boolean savePlayer(RegisterUserDTO userDTO) {

        return userRepo.save(new Player(userDTO.getUserName(), null)).getPlayerId() != 0;
    }

    @Override
    public ResponseEntity<String> generateResetPassToken(String userName) {
        final String randomString = userName + UUID.randomUUID() + System.currentTimeMillis();
        final String token = encoder.encode(randomString);
        CacheObject cacheObject
                = new CacheObject(userName, System.currentTimeMillis());
        appCache.put(token, cacheObject);
        return ResponseEntity.ok().body(token);
    }

    @Override
    public ResponseEntity<String> resetPassword(ResetUserRequest resetUserRequest) {
        String result = "Reset password for user " + resetUserRequest.getUserName() + " successfully.";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(result);
        try {
            validateRequest(resetUserRequest);
            Player user = userRepo.findByPlayerName(resetUserRequest.getUserName()).get();
            user.setPassword(encoder.encode(resetUserRequest.getNewPass()));
            userRepo.save(user);
            appCache.remove(resetUserRequest.getResetToken());
        } catch (IllegalArgumentException e) {
            responseEntity = ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            responseEntity = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
        return responseEntity;
    }

    private void validateRequest(ResetUserRequest resetUserRequest) {
        Assert.isTrue(appCache.contains(resetUserRequest.getResetToken()), "Token is invalid");
        CacheObject cacheObject = (CacheObject) appCache.get(resetUserRequest.getResetToken());
        Assert.isTrue((System.currentTimeMillis() - cacheObject.getExpiryTime()) < 1000 * 60 * 3, "Token is expired.");
        Assert.isTrue(resetUserRequest.getUserName().equals(cacheObject.getValue()),
                "User is not present.");
        Assert.isTrue(resetUserRequest.getNewPass().equals(resetUserRequest.getRepeatNewPass()),
                "Two passwords must match.");

    }

}
