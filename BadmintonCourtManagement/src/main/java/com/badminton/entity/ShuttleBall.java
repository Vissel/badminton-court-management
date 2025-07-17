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
@Table(name = "shuttle_ball")
@Getter
@Setter
@NoArgsConstructor
public class ShuttleBall {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int shuttleId;

	private String shuttleName;

	private float cost;

	@Column(updatable = false, insertable = false)
	private Timestamp createdDate;

	private boolean isActive;

	public ShuttleBall(String name, float cost) {
		super();
		this.shuttleName = name;
		this.cost = cost;
	}

}
