package com.cosync.identity.repository;

import java.util.Optional; 

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cosync.identity.entity.EmailOtp;
import com.cosync.identity.entity.User;

@Repository
public interface EmailOtpRepository  extends JpaRepository<EmailOtp, Long> {

    //SELECT * FROM email_otp WHERE user_id = ? ORDER BY created_at DESC LIMIT 1
    Optional<EmailOtp> findFirstByUserOrderByCreatedAtDesc(User user);

}
