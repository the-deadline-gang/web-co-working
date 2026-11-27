package com.cosync.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.booking.dto.zone.CreateZoneRequest;
import com.cosync.booking.dto.zone.ZoneResponse;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.service.ZoneService;

import jakarta.validation.Valid;



@RestController 
@RequestMapping("/admin/zones")
public class AdminZoneController {
    private final ZoneService zoneService;

    public AdminZoneController(ZoneService zoneService){
        this.zoneService = zoneService;
    }

    @GetMapping
    @RequirePermission("resource.manage")
    public List<ZoneResponse> get(){
        return  zoneService.listZones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission("resource.manage")
    public ZoneResponse createZone( @Valid @RequestBody  CreateZoneRequest req){
        return zoneService.createZone(req);
    }
}
