package com.badminton.service;

import com.badminton.requestmodel.RegisterUserDTO;

public interface UserService {

	public boolean saveAdminUser(RegisterUserDTO userDTO);

	public boolean savePlayer(RegisterUserDTO userDTO);

}
