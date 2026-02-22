package com.badminton.model.dto;

import com.badminton.entity.ShuttleBall;
import com.badminton.requestmodel.ResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShuttleBallDTO extends ResponseDTO {
    private String shuttleName;
    private float shuttleCost;
    private int ballQuantity;
    private boolean selected;

    public ShuttleBallDTO(ShuttleBall ballEntity) {
        this.shuttleName = ballEntity.getShuttleName();
        this.shuttleCost = ballEntity.getCost();
        this.selected = ballEntity.isSelected();
    }
}
