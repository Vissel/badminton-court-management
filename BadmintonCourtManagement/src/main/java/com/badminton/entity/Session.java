package com.badminton.entity;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "`session`", schema = "`bad-court-management-db`")
@Getter
@Setter
//@NoArgsConstructor
public class Session {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int sessionId;

	@Column(updatable = false, insertable = false)
	private Instant fromTime;

	@Column(updatable = true, insertable = true, nullable = true)
	private Instant toTime;

	private boolean isActive;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "session")
	private List<AvailablePlayer> availablePlayers;

	public Session() {
		setActive(true);
	}
}
