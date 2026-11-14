package com.cosync.wallet.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.wallet.repository.TopupRepository;
@Component
public class TopupExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(TopupExpiryJob.class);

    private final TopupRepository topupRepository;

    public TopupExpiryJob(TopupRepository topupRepository) {
        this.topupRepository = topupRepository;
    }

    /** W-09: ทุก 1 นาที เปลี่ยน PENDING ที่เลยเวลาแล้วเป็น EXPIRED */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expireOverdueTopups() {
        int count = topupRepository.expireOverdue(Instant.now());
        if (count > 0) {
            log.info("เปลี่ยน topup ที่หมดอายุเป็น EXPIRED {} รายการ", count);
        }
    }
}