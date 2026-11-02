package com.badminton.repository;

import com.badminton.entity.Invoice;
import com.badminton.enums.InvoiceStatus;
import com.badminton.enums.InvoiceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

        Optional<Invoice> findByBillNo(String billNo);

        /** Idempotency lookup: one bill per client payment attempt. */
        Optional<Invoice> findByClientTxnId(String clientTxnId);

        /** True once a correction (điều chỉnh/thay thế) bill points at this one. */
        boolean existsByRelatedInvoiceInvoiceId(Long invoiceId);

        List<Invoice> findByRelatedInvoiceInvoiceIdOrderByInvoiceIdAsc(Long invoiceId);

        @Query("""
                        SELECT i FROM Invoice i
                        WHERE (:from IS NULL OR i.issuedAt >= :from)
                          AND (:to IS NULL OR i.issuedAt < :to)
                          AND (:status IS NULL OR i.status = :status)
                          AND (:invoiceType IS NULL OR i.invoiceType = :invoiceType)
                          AND (:sessionId IS NULL OR i.session.sessionId = :sessionId)
                          AND (:keyword IS NULL OR LOWER(i.billNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(i.buyerName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(i.buyerCompany) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(i.buyerTaxCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                               OR LOWER(i.player.playerName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                        ORDER BY i.issuedAt DESC
                        """)
        Page<Invoice> search(@Param("from") Instant from,
                        @Param("to") Instant to,
                        @Param("status") InvoiceStatus status,
                        @Param("invoiceType") InvoiceType invoiceType,
                        @Param("sessionId") Integer sessionId,
                        @Param("keyword") String keyword,
                        Pageable pageable);

        /**
         * Rows that count towards declared revenue: everything in range except
         * voided bills (a voided bill keeps its number but not its revenue).
         */
        @Query("""
                        SELECT i.issuedAt, i.total, i.subtotal, i.vatAmount
                        FROM Invoice i
                        WHERE i.issuedAt >= :from
                          AND i.issuedAt < :to
                          AND i.status <> :voidedStatus
                        ORDER BY i.issuedAt ASC
                        """)
        List<Object[]> findReportRows(@Param("from") Instant from,
                        @Param("to") Instant to,
                        @Param("voidedStatus") InvoiceStatus voidedStatus);
}
