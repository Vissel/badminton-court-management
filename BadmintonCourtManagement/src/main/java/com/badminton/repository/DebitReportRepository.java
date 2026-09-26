package com.badminton.repository;

import com.badminton.entity.Debit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DebitReportRepository extends JpaRepository<Debit, Integer>, DebitReportRepositoryCustom {
}
