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
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByBillNo(String billNo);

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
}
