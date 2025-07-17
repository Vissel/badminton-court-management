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
@Table(name = "court")
@Getter
@Setter
@NoArgsConstructor
public class Court {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int courtId;

	private String courtName;

	private boolean isActive;

	@Column(updatable = false, insertable = false)
	private Timestamp createdDate;

	public Court(String name) {
		super();
		this.courtName = name;
		this.isActive = true; // default value
	}
}
