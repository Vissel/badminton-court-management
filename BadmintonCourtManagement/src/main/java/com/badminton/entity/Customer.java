package com.badminton.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Buyer directory for repeat/B2B customers. Optional on a POS invoice — the
 * buyer block is only required when the customer asks for it — so this table
 * exists purely to speed up checkout for regulars.
 */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long customerId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 200)
    private String company;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(length = 300)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(name = "bank_name", length = 120)
    private String bankName;

    @Column(name = "bank_account", length = 40)
    private String bankAccount;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private Instant createdAt;

    public Customer(String name) {
        this.name = name;
    }
}
