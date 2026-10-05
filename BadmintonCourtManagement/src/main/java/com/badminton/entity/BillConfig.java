package com.badminton.entity;

import com.badminton.enums.PrinterMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Single-row venue billing profile: seller identity printed on bill headers,
 * default VAT rate, receipt footer and print/e-invoice settings.
 */
@Entity
@Table(name = "bill_config")
@Getter
@Setter
@NoArgsConstructor
public class BillConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int configId;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(length = 300)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(name = "bill_prefix", nullable = false, length = 10)
    private String billPrefix = "BL";

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "bill_footer", length = 250)
    private String billFooter;

    @Enumerated(EnumType.STRING)
    @Column(name = "printer_mode", nullable = false, length = 15)
    private PrinterMode printerMode = PrinterMode.BROWSER;

    @Column(name = "printer_ip", length = 45)
    private String printerIp;

    @Column(name = "printer_port")
    private Integer printerPort = 9100;

    @Column(name = "paper_width")
    private Integer paperWidth = 80;

    @Column(name = "einvoice_enabled", nullable = false)
    private boolean einvoiceEnabled = false;

    @Column(name = "einvoice_series", length = 20)
    private String einvoiceSeries;

    @Column(name = "einvoice_template", length = 50)
    private String einvoiceTemplate;

    @Column(name = "auto_print", nullable = false)
    private boolean autoPrint = true;

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
}
