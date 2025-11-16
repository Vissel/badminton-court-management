package com.badminton.entity;

import com.badminton.requestmodel.ShuttleBallDTO;
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

    @Column(name = "is_selected")
    private boolean isSelected;

    public ShuttleBall(String name, float cost) {
        super();
        this.shuttleName = name;
        this.cost = cost;
        this.isActive = true;
    }

    /**
     * check this ShuttleBall has the same to ball DTO but name, and cost.
     *
     * @param ballDTO
     * @return
     */
    public boolean theSameDTO(ShuttleBallDTO ballDTO) {
        return this.shuttleName.equals(ballDTO.getShuttleName()) && this.cost == ballDTO.getShuttleCost();
    }
}
