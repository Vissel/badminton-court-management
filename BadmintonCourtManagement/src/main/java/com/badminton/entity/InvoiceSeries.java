package com.badminton.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gapless bill numbering. The row is locked with SELECT ... FOR UPDATE inside
 * the payment transaction before incrementing, so concurrent checkouts never
 * reuse or skip a number unless the whole payment rolls back.
 */
@Entity
@Table(name = "invoice_series")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceSeries {
    @Id
    @Column(name = "series_key", length = 20)
    private String seriesKey;

    @Column(name = "current_no", nullable = false)
    private long currentNo = 0;

    public InvoiceSeries(String seriesKey) {
        this.seriesKey = seriesKey;
    }
}
