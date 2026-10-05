package com.badminton.requestmodel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserDTO {

	private String userId;
	private String userName;
	/** Base64 RSA-OAEP ciphertext — decrypted server-side before encoding. */
	private String password;
	private String name;
	private String link;
	private String role;
}
