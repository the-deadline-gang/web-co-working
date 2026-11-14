package com.cosync.wallet.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cosync.wallet.entity.Topup;
import com.cosync.wallet.entity.TopupStatus;

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


    @Query("""
        select t from Topup t
         where (:status is null or t.status = :status)
           and t.createdAt >= :start and t.createdAt < :end
        """)
    Page<Topup> searchForAdmin(@Param("status") TopupStatus status,
                            @Param("start") Instant start,
                            @Param("end") Instant end,
                            Pageable pageable);

    /** topup ที่ยังรอผลการจ่าย (PENDING / EXPIRED) ซึ่งสร้างหลังเวลาที่กำหนด ใช้ให้ job ไล่ถาม gateway */
    List<Topup> findByStatusInAndCreatedAtAfter(Collection<TopupStatus> statuses, Instant since);
}