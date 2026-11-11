package com.cosync.wallet.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cosync.wallet.entity.Topup;

import jakarta.persistence.LockModeType;

public interface TopupRepository extends JpaRepository<Topup, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Topup t where t.gatewayRef = :gatewayRef")
    Optional<Topup> findByGatewayRefForUpdate(@Param("gatewayRef") String gatewayRef);


    @Modifying
    @Query("""
            update Topup t
            set t.status = com.cosync.wallet.entity.TopupStatus.EXPIRED,
                t.updatedAt = :now
            where t.status = com.cosync.wallet.entity.TopupStatus.PENDING
            and t.expiresAt < :now
            """)
    int expireOverdue(@Param("now") Instant now);
}