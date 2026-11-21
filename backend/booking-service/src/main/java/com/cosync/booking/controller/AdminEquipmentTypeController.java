package com.cosync.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.booking.dto.equipment.CreateEquipmentTypeRequest;
import com.cosync.booking.dto.equipment.EquipmentPricingRequest;
import com.cosync.booking.dto.equipment.EquipmentTypeResponse;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.service.EquipmentTypeService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/admin/equipment-types")
public class AdminEquipmentTypeController {
    private final EquipmentTypeService equipmentTypeService;

    public AdminEquipmentTypeController(EquipmentTypeService equipmentTypeService){
        this.equipmentTypeService = equipmentTypeService;
    }

    @GetMapping()
    @RequirePermission("equipment.manage")
    public List<EquipmentTypeResponse> listEquipment() {
        return  equipmentTypeService.listEquipmentType();
    }

    @PostMapping()
    @RequirePermission("equipment.manage")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentTypeResponse createEquipmentType( @Valid @RequestBody  CreateEquipmentTypeRequest req) {
       return   equipmentTypeService.createEquipmentType(req);
    }

    
    @PutMapping("/{id}/pricing")
    @RequirePermission("rate.manage")
    public EquipmentTypeResponse setPricing(@RequestHeader("X-User-Id") Long staffId,
                                @PathVariable Long id,
                                @Valid @RequestBody EquipmentPricingRequest req) {
        return  equipmentTypeService.setPricing(staffId, id, req);
    } 
    

}
