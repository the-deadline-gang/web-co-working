package com.cosync.booking.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.booking.dto.equipment.EquipmentRequest;
import com.cosync.booking.dto.equipment.EquipmentResponse;
import com.cosync.booking.entity.enumtype.EquipmentStatus;
import com.cosync.booking.security.RequirePermission;
import com.cosync.booking.service.EquipmentService;

import jakarta.validation.Valid;




@RestController
@RequestMapping("/admin/equipment")
public class AdminEquipmentController {

    private final EquipmentService equipmentService;

    public AdminEquipmentController(EquipmentService equipmentService){
        this.equipmentService = equipmentService;
    }

    @GetMapping()
    @RequirePermission("equipment.manage")
    public List<EquipmentResponse> listEquipment(@RequestParam(required = false) EquipmentStatus status , @RequestParam(required = false) Long typeId) {
        return equipmentService.listEquipment(status,typeId);
    }

    @PostMapping()
    @RequirePermission("equipment.manage")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse crateEquipment( @Valid @RequestBody  EquipmentRequest req) {
        return equipmentService.createEquipment(req);
    }
    
    @PutMapping("/{equipmentId}")
    @RequirePermission("equipment.manage")
    public EquipmentResponse updateEquipment(@PathVariable Long equipmentId,  @Valid @RequestBody  EquipmentRequest req) {
        return equipmentService.updateEquipment(equipmentId, req);
    }

    @DeleteMapping("/{equipmentId}")
    @RequirePermission("equipment.manage")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEquipment(@PathVariable Long equipmentId) {
        equipmentService.deleteEquipment(equipmentId);
    }
}
