package com.badminton.service;

import com.badminton.requestmodel.RegisterUserDTO;

public interface UserService {

    boolean saveAdminUser(RegisterUserDTO userDTO);

    boolean savePlayer(RegisterUserDTO userDTO);

    boolean createAdminUser(RegisterUserDTO requestCreateUser);
}
