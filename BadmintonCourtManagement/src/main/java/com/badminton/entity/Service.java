package com.badminton.entity;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "service", uniqueConstraints = {
		@UniqueConstraint(name = "unique_service_name", columnNames = { "ser_name", "is_active" }) })
@Getter
@Setter
@NoArgsConstructor
public class Service {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int serId;

	@Column(name = "ser_name")
	private String serName;

	private float cost;

	@Column(updatable = false, insertable = false)
	private Timestamp createdDate;

	@Column(name = "is_active")
	private boolean isActive;

	public Service(String name, float cost) {
		super();
		this.serName = name;
		this.cost = cost;
		isActive = true;
	}

}
