package com.badminton.repository;

import com.badminton.entity.InvoiceSeries;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceSeriesRepository extends JpaRepository<InvoiceSeries, String> {

    /**
     * Pessimistic write lock — the caller must be inside the payment
     * transaction so concurrent checkouts serialize on the series row.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<InvoiceSeries> findBySeriesKey(String seriesKey);
}
