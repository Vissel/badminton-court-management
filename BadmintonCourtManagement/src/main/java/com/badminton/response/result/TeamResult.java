package com.badminton.response.result;

import com.badminton.constant.GameConstant;
import lombok.Data;

@Data
public class TeamResult {
    private String playerOneName;

    private float expenseOne = 0.0f;

    private String playerTwoName;

    private float expenseTwo = 0.0f;

    private String win = GameConstant.WIN;
}
