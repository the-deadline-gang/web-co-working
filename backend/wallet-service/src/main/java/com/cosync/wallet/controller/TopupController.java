package com.cosync.wallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.wallet.dto.topup.TopupConfigResponse;
import com.cosync.wallet.service.TopupService;

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
}