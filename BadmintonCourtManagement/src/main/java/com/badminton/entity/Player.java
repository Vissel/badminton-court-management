package com.badminton.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "player")
@Getter
@Setter
@NoArgsConstructor
public class Player {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int playerId;

	private String playerName;

	private String password;

	@Column(updatable = false, insertable = false)
	private Timestamp createdDate;

	public Player(String playerName, String playerPassword) {
		super();
		this.playerName = playerName;
		this.password = playerPassword;
	}

}
