package com.badminton.core.billing.einvoice;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MISA meInvoice credentials and endpoints — bound from
 * {@code einvoice.misa.*} application properties. Never exposed through the
 * API (BillConfigResponse deliberately omits them).
 */
@Data
@Component
@ConfigurationProperties(prefix = "einvoice.misa")
public class EInvoiceProperties {

    private String baseUrl = "https://api.meinvoice.vn";
    private String appId;
    private String taxCode;
    private String username;
    private String password;
    private int timeoutSeconds = 15;

    private String tokenPath = "/api/integration/token";
    private String publishPath = "/api/integration/invoice";
    private String statusPath = "/api/integration/invoice/status";
    private String downloadPath = "/api/integration/invoice/download";
    private String sendMailPath = "/api/integration/invoice/send-mail";

    public boolean isConfigured() {
        return isNotBlank(appId) && isNotBlank(taxCode)
                && isNotBlank(username) && isNotBlank(password);
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
