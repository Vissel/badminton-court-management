package com.badminton.repository;

import com.badminton.entity.InvoiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceLogRepository extends JpaRepository<InvoiceLog, Long> {

    List<InvoiceLog> findByInvoiceIdOrderByLogIdDesc(Long invoiceId);

    List<InvoiceLog> findByInvoiceIdOrderByLogIdAsc(Long invoiceId);
}
