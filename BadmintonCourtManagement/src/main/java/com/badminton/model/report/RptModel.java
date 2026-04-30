package com.badminton.model.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public abstract class RptModel {
    protected final String FORMAT = "%s - %s";

    private float price;
    private String priceFormat;
    private int quantity;

    public abstract String rptModelDisplay();
}
