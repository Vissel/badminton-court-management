package com.badminton.entity;

import com.badminton.constant.CommonConstant;
import com.badminton.util.CommonUtil;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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

    private Float payAmount;

    private String payType;

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
