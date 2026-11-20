package com.cosync.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.booking.dto.setting.BookingSettingsRequest;
import com.cosync.booking.dto.setting.BookingSettingsResponse;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.service.SettingService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/admin/settings")
public class AdminSettingController {


    private final SettingService settingService;

    public AdminSettingController(SettingService settingService){
        this.settingService = settingService;
    }

    @GetMapping
    @RequirePermission("setting.manage")
    public BookingSettingsResponse getSettings(){
        return  settingService.getSettings();
    }

    @PutMapping
    @RequirePermission("setting.manage")
    public BookingSettingsResponse updateSettings(@RequestHeader("X-User-Id") Long staffId,
                                                     @Valid @RequestBody BookingSettingsRequest req){
        return  settingService.updateSettings(staffId, req);
    }
}
