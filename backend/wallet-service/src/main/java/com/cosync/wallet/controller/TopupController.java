package com.cosync.wallet.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.topup.CreateTopupRequest;
import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.dto.topup.TopupResponse;
import com.cosync.wallet.security.MemberOnly;
import com.cosync.wallet.service.TopupService;

import jakarta.validation.Valid;

// review 10/11/2026 by peak
@RestController
@RequestMapping("/topups")
public class TopupController {

    private final TopupService topupService;

    public TopupController(TopupService topupService) {
        this.topupService = topupService;
    }

    @GetMapping("/config")          // GET /topups/config
    public TopupConfigResponse getConfig() {
        return topupService.getConfig();
    }

    @MemberOnly
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TopupResponse create(@RequestHeader("X-User-Id") Long userId,
                                @Valid @RequestBody CreateTopupRequest req) {
        return topupService.create(userId, req);
    }


    @MemberOnly
    @GetMapping("/{topupId}")
    public TopupResponse getTopup(
            @PathVariable Long topupId,
            @RequestHeader("X-User-Id") Long userId) {
        return topupService.get(userId,topupId);
    }

}