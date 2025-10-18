package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "shuttle_ball")
@Getter
@Setter
@NoArgsConstructor
public class ShuttleBall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int shuttleId;

    @Column(name = "shuttle_name")
    private String shuttleName;

    private float cost;

    @Column(updatable = false, insertable = false)
    private Timestamp createdDate;

    @Column(name = "is_active")
    private boolean isActive;

    public ShuttleBall(String name, float cost) {
        super();
        this.shuttleName = name;
        this.cost = cost;
        this.isActive = true;
    }
}
