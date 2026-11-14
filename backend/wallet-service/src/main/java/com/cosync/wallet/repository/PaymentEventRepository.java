package com.cosync.wallet.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cosync.wallet.entity.PaymentEvent;

@Repository 
public interface PaymentEventRepository extends JpaRepository<PaymentEvent,String> {
    
}
