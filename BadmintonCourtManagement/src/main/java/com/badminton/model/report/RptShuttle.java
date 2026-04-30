package com.badminton.model.report;

import lombok.Data;

@Data
public class RptShuttle extends RptModel {

    private String shuttleName;

    @Override
    public String rptModelDisplay() {
        return String.format(FORMAT, shuttleName, getPriceFormat());
    }
}
