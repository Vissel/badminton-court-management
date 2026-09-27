package com.badminton.response.result;

import com.badminton.entity.ShuttleBall;
import com.badminton.util.MoneyUtils;
import lombok.Data;

import java.util.Objects;

@Data
public class ShuttleBallResponse {
    private String shuttleName;
    private float cost;
    private String costFormat;
    private final String currency = MoneyUtils.CURRENCY_VN;
    private boolean selected;

    /** Linked stock item id; null when the ball is not linked to inventory. */
    private Integer itemId;

    /** Current stock in base units (quả); null when the ball is not linked to inventory. */
    private Long stockOnHand;

    /** True when stockOnHand is non-null and <= 10. */
    private Boolean lowStock;

    public ShuttleBallResponse(String shuttleName, float cost, boolean isSelected) {
        this.shuttleName = shuttleName;
        this.cost = cost;
        this.costFormat = MoneyUtils.formatToVND(cost);
        this.selected = isSelected;
    }

    public ShuttleBallResponse(ShuttleBall ball) {
        this(ball.getShuttleName(), ball.getCost(), ball.isSelected());
        this.itemId = ball.getItem() != null ? ball.getItem().getItemId() : null;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShuttleBallResponse)) return false;
        ShuttleBallResponse
                that = (ShuttleBallResponse) o;
        return cost == that.cost && Objects.equals(shuttleName, that.shuttleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shuttleName, cost);
    }
}
