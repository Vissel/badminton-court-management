package com.badminton.requestmodel;

import com.google.gson.Gson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenDTO {
	public String message;
	public String username;
	public String csrfToken;
	public boolean valid;
	public long expiresInSeconds;

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
}
