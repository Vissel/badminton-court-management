package com.badminton.requestmodel.billing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Editable fields of the single-row bill_config. Null fields are left
 * unchanged so partial updates are safe.
 */
@Data
public class BillConfigRequest {
    private String businessName;
    private String taxCode;
    private String address;
    private String phone;
    private String billPrefix;
    private BigDecimal vatRate;
    private String billFooter;
    private String printerMode;
    private String printerIp;
    private Integer printerPort;
    private Integer paperWidth;
    private Boolean einvoiceEnabled;
    private String einvoiceSeries;
    private String einvoiceTemplate;
    private Boolean autoPrint;
}
