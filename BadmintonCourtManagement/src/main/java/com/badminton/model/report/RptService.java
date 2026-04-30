package com.badminton.model.report;

import lombok.Data;

@Data
public class RptService extends RptModel {
    private String serviceName;

    @Override
    public String rptModelDisplay() {
        return String.format(FORMAT, serviceName, getPriceFormat());
    }
}
