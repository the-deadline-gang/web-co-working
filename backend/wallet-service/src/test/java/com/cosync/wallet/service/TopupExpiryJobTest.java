package com.cosync.wallet.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cosync.wallet.repository.TopupRepository;

@ExtendWith(MockitoExtension.class)
class TopupExpiryJobTest {

    @Mock TopupRepository topupRepository;

    @InjectMocks TopupExpiryJob job;

    @Test
    @DisplayName("TC-EXP-01 job ส่งเวลาปัจจุบันไปให้ bulk update")
    void passesCurrentTime() {
        when(topupRepository.expireOverdue(any())).thenReturn(2);
        Instant before = Instant.now();

        job.expireOverdueTopups();

        ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
        verify(topupRepository).expireOverdue(now.capture());
        assertTrue(!now.getValue().isBefore(before) && !now.getValue().isAfter(Instant.now()));
    }

    @Test
    @DisplayName("TC-EXP-02 ไม่มีรายการหมดอายุ → ไม่พัง")
    void nothingToExpire() {
        when(topupRepository.expireOverdue(any())).thenReturn(0);

        job.expireOverdueTopups();

        verify(topupRepository).expireOverdue(any());
    }
}
