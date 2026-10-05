package com.badminton.model.billing;

import lombok.Data;

/**
 * Buyer identity printed on the bill. All fields optional — walk-in players
 * only carry a name; business customers add company/tax code for VAT bills.
 */
@Data
public class BuyerInfo {
    private String buyerName;
    private String company;
    private String taxCode;
    private String address;
    private String email;
}
