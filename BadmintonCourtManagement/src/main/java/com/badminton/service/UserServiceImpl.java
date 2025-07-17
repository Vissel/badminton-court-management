package com.badminton.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.badminton.entity.Player;
import com.badminton.repository.UserRepository;
import com.badminton.requestmodel.RegisterUserDTO;

@Service
public class UserServiceImpl implements UserService {
	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private UserRepository userRepo;

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

}
