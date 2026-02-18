package com.badminton.entity;

import com.badminton.requestmodel.ServiceDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "service")
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

    public boolean isTheSame(ServiceDTO serviceDTO) {
        return serviceDTO.getServiceName().equals(serName) && serviceDTO.getCost() == cost;
    }

    public Service setActiveService() {
        this.setActive(true);
        return this;
    }

    public Service setDeActiveService() {
        this.setActive(false);
        return this;
    }

}
