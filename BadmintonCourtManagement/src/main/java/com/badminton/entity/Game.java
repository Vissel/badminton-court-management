package com.badminton.entity;

import java.sql.Timestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "game")
@Getter
@Setter
@NoArgsConstructor
public class Game {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int gameId;

	private String courtId;

	private String shuttleId;

	private int shuttleNumber;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "team_id1", nullable = false)
	private Team teamOne;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "team_id2", nullable = false)
	private Team teamTwo;

	@Column(updatable = false, insertable = false)
	private Timestamp createdDate;

	@Column(nullable = true)
	private Timestamp endedDate;

	private String state;

}