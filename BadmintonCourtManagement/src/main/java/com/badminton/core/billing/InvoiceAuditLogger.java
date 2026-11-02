package com.badminton.core.billing;

import com.badminton.entity.Invoice;
import com.badminton.entity.InvoiceLog;
import com.badminton.enums.InvoiceLogAction;
import com.badminton.repository.InvoiceLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

/**
 * Writes the internal-control trail. Never throws: a logging failure must not
 * roll a bill/payment back — it is reported to the server log instead.
 */
@Slf4j
@Component
public class InvoiceAuditLogger {

    private static final int DETAIL_MAX = 250;
    private static final int PAYLOAD_MAX = 4000;
    private static final int ERROR_MAX = 500;

    @Autowired
    private InvoiceLogRepository invoiceLogRepository;

    public void log(InvoiceLogAction action, Invoice invoice, String detail) {
        log(action, invoice, detail, null, null);
    }

    public void log(InvoiceLogAction action, Invoice invoice, String detail, String payload, String error) {
        try {
            InvoiceLog entry = new InvoiceLog(action);
            if (invoice != null) {
                entry.setInvoiceId(invoice.getInvoiceId());
                entry.setBillNo(invoice.getBillNo());
            }
            entry.setActor(currentUser());
            entry.setIp(clientIp());
            entry.setDetail(truncate(detail, DETAIL_MAX));
            entry.setPayload(truncate(payload, PAYLOAD_MAX));
            entry.setError(truncate(error, ERROR_MAX));
            entry.setCreatedAt(Instant.now());
            invoiceLogRepository.save(entry);
        } catch (RuntimeException e) {
            log.warn("Cannot write invoice log for action {}: {}", action, e.getMessage());
        }
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && StringUtils.isNotBlank(auth.getName()) ? auth.getName() : "system";
    }

    private String clientIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
