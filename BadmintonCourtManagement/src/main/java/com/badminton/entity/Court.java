package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.List;

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

    public Court(int id, String name) {
        this(name);
        this.courtId = id;
    }

    @OneToMany(mappedBy = "court")
    public List<Game> games;

}
