package com.badminton.response.result;

import com.badminton.constant.GameConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamResult {
    private String playerOneName;

    private float expenseOne = 0.0f;

    private String playerTwoName;

    private float expenseTwo = 0.0f;

    /**
     * Default is lose team
     */
    private String win = GameConstant.LOSE;
}
