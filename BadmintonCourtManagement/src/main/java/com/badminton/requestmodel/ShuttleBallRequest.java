package com.badminton.requestmodel;

import com.badminton.entity.ShuttleBall;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShuttleBallRequest extends ResponseDTO {
    private String shuttleName;
    private float cost;

    public ShuttleBallRequest(ShuttleBall ballEntity) {
        this.shuttleName = ballEntity.getShuttleName();
        this.cost = ballEntity.getCost();
    }
}
