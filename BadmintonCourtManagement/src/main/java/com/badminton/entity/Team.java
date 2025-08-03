package com.badminton.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int teamId;

	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "player_id1")
	private Player playerOne;

	@Column(name = "expense_1")
	private float expenseOne;

	@ManyToOne(cascade = CascadeType.DETACH)
	@JoinColumn(name = "player_id2")
	private Player playerTwo;

	@Column(name = "expense_2")
	private float expenseTwo;

	@OneToOne
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;
}
