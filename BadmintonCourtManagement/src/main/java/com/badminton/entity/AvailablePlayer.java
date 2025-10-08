package com.badminton.entity;

import java.time.Instant;

import com.badminton.constant.CommonConstant;
import com.badminton.util.CommonUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "available_player")
@Getter
@Setter
@NoArgsConstructor
public class AvailablePlayer {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long avaId;

	@ManyToOne
	@JoinColumn(name = "player_id", nullable = false)
	private Player player;

	@ManyToOne
	@JoinColumn(name = "session_id", nullable = false)
	private Session session;

	@Column(updatable = true, insertable = true, nullable = true)
	private Instant leaveTime;

	private String services;

	public AvailablePlayer(Player p) {
		this.player = p;
	}

	public AvailablePlayer(Player p, Session s) {
		this(p);
		this.session = s;
	}

	/**
	 * Make sure the service has not null
	 * 
	 * @return
	 */
	public String getCurrentServices() {
		return CommonUtil.isNotNullEmpty(this.services) ? this.services : CommonConstant.EMPTY;
	}

}
