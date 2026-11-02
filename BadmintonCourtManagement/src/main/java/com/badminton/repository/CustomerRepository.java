package com.badminton.repository;

import com.badminton.entity.Customer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** Checkout autocomplete by name, company, MST or phone. */
    @Query("""
            SELECT c FROM Customer c
            WHERE c.active = true
              AND (:keyword IS NULL
                   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.company) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.taxCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR c.phone LIKE CONCAT('%', :keyword, '%'))
            ORDER BY c.name ASC
            """)
    List<Customer> search(@Param("keyword") String keyword, Pageable pageable);
}
