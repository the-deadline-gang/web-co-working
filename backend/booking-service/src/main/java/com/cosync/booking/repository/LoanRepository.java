package com.cosync.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.booking.entity.Loan;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    
}
