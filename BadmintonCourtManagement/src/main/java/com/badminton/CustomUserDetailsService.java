package com.badminton;

import com.badminton.entity.AppUser;
import com.badminton.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
	private final AppUserRepository userRepository;

	public CustomUserDetailsService(AppUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		AppUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
				.password(user.getPassword())
				.disabled(!user.isActive())
				.authorities(user.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRoleName().name()))
						.toList())
				.build();
	}
}
