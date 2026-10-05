package com.badminton.response.billing;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Bill configuration returned to the FE. MISA credentials are deliberately
 * absent — they live in application properties, never in the DB or the API.
 */
@Data
public class BillConfigResponse {
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
    private boolean einvoiceEnabled;
    private String einvoiceSeries;
    private String einvoiceTemplate;
    private boolean autoPrint;
}
