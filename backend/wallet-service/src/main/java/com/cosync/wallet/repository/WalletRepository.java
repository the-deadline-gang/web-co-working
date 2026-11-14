package com.cosync.wallet.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cosync.wallet.entity.Wallet;

import jakarta.persistence.LockModeType;

@Repository 
public interface WalletRepository extends JpaRepository<Wallet,Long>  {
    Optional<Wallet> findByUserId(Long userId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)                  //  ล็อกแถว
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") Long id);

    /** W-05: debit/credit รู้แค่ userId จึงล็อกด้วย userId */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userId = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
