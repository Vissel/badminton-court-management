package com.badminton.response.result;

import com.badminton.constant.GameConstant;
import com.badminton.util.MoneyUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamResult {
    private String playerOneName;

    private float expenseOne = MoneyUtils.DEFAULT;

    private String playerTwoName;

    private float expenseTwo = MoneyUtils.DEFAULT;

    /**
     * Default is lose team
     */
    private String win = GameConstant.LOSE;
}
