package com.badminton.response.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShuttleBallResult {
    private String shuttleName;

    private float cost;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShuttleBallResult)) return false;
        ShuttleBallResult
                that = (ShuttleBallResult) o;
        return cost == that.cost && Objects.equals(shuttleName, that.shuttleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shuttleName, cost);
    }
}
