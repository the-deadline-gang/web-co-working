package com.cosync.booking.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.booking.dto.rate.RateResponse;
import com.cosync.booking.dto.rate.RateUpdateRequest;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.service.RateService;

import jakarta.validation.Valid;



@RestController 
@RequestMapping("/admin/rates")
public class AdminRateController {
    
    private final RateService rateService;

    public AdminRateController(RateService rateService){
        this.rateService = rateService;
     
    }

    @GetMapping
    @RequirePermission("rate.manage")
    public List<RateResponse> getRate(){
        return rateService.list();
    }

    @PutMapping("/{serviceType}")
    @RequirePermission("rate.manage")
    public RateResponse updateRate(@PathVariable ServiceType serviceType,
                                   @RequestHeader("X-User-Id") Long staffId,
                                   @Valid @RequestBody RateUpdateRequest req) {

        return rateService.update(staffId,serviceType, req);
    }

}
