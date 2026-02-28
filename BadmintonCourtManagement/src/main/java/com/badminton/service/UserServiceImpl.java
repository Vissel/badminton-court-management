package com.badminton.service;

import com.badminton.entity.Player;
import com.badminton.integration.UserClient;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.RegisterUserDTO;
import com.qrpublic.apartment.template.model.Result;
import com.qrpublic.apartment.user.interfaces.request.UserCreateRequest;
import com.qrpublic.apartment.user.interfaces.response.UserCreateResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserClient userClient;

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
    public boolean createAdminUser(RegisterUserDTO requestCreateUser) {
        UserCreateRequest userCreateRequest = new UserCreateRequest();
        userCreateRequest.setUserName(requestCreateUser.getUserName());
        userCreateRequest.setEncryptedPassword(requestCreateUser.getPassword());
        userCreateRequest.setRole("Badminton_Admin");
        ResponseEntity<Result<UserCreateResponse>> resultEntity = userClient.createUser(userCreateRequest);
        return HttpStatus.OK.equals(resultEntity.getStatusCode());
    }

}
