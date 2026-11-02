package com.badminton.requestmodel.billing;

import lombok.Data;

/**
 * Resend the issued e-invoice to the buyer. Empty body → the buyer email
 * stored on the bill is used.
 */
@Data
public class SendEInvoiceRequest {
    private String email;
}
