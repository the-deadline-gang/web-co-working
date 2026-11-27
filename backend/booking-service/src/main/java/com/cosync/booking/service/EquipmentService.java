package com.cosync.booking.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.equipment.EquipmentRequest;
import com.cosync.booking.dto.equipment.EquipmentResponse;
import com.cosync.booking.entity.Equipment;
import com.cosync.booking.entity.EquipmentType;
import com.cosync.booking.entity.Resource;
import com.cosync.booking.entity.enumtype.EquipmentStatus;
import com.cosync.booking.repository.EquipmentRepository;
import com.cosync.booking.repository.EquipmentTypeRepository;
import com.cosync.booking.repository.LoanRepository;
import com.cosync.booking.repository.ResourceRepository;

@Service 
public class EquipmentService {
    
    private final EquipmentRepository equipmentRepository;
    private final ResourceRepository resourceRepository;
    private final EquipmentTypeRepository equipmentTypeRepository;
    private final LoanRepository loanRepository;

    public EquipmentService(EquipmentRepository equipmentRepository,ResourceRepository resourceRepository,EquipmentTypeRepository equipmentTypeRepository,LoanRepository loanRepository){
        this.equipmentRepository = equipmentRepository;
        this.resourceRepository = resourceRepository;
        this.equipmentTypeRepository = equipmentTypeRepository;
        this.loanRepository = loanRepository;
    }

    /**
     * เอาไว้ดึงรายการของ Equipment ทั้งหมด
     * @return ส่งรายการ Equipment ไปแบบ EquipmentResponse ที่เป็น list
     */
    @Transactional(readOnly = true)
    public List<EquipmentResponse> listEquipment(EquipmentStatus status , Long typeId){
        return  equipmentRepository.findAll().stream().filter(e -> status == null || e.getStatus() == status)
                                  .filter(e -> typeId == null || e.getEquipmentType().getId().equals(typeId))
                                  .map(EquipmentResponse::from)
                                  .toList();
    }

    /**
     * เอาไว้สร้าง Equipment ใหม่ method นี้เป็นแบบ Trasactionnal
     * โดยมีการค้นหา resourceId กับ equiomentId ก่อนที่จะทำการสร้าง  Equipment ใหม่
     * @param req รับมามาแบบ EquipmentRequest
     * @return ส่งรายการไปแบบ EquipmentResponse
     */
    @Transactional 
    public EquipmentResponse createEquipment(EquipmentRequest req){
        Resource resource = null;
        if (req.resourceId() != null) {
            resource = resourceRepository.findById(req.resourceId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND","ไม่เจอ "+req.resourceId()));
        }
        EquipmentType equipmentType = equipmentTypeRepository.findById(req.equipmentTypeId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EQUIPMENT_TYPE_NOT_FOUND","ไม่เจอ "+req.equipmentTypeId()+" ของ EQUIPMENTTYPE "));
        Equipment equipment = new Equipment(equipmentType, req.barcode().trim(), resource);

        equipmentRepository.save(equipment);

        return EquipmentResponse.from(equipment);
    }

    /**
     * เอาไว้ update Equipment  method นี้เป็นแบบ Trasactionnal
     * โดยมีการค้นหา resourceId , equipmentId กับ equiomentId ก่อนที่จะทำการ update Equipment ใหม่
     * มีการค้นหาว่า Equipment ถูกยืมอยู่ไหมถ้าถูกยืมแล้วจะไม่ให้ update
     * @param equipmentId id ของ Equipment ที่จะไป update
     * @param req รับมามาแบบ EquipmentRequest
     * @return ส่งรายการไปแบบ EquipmentResponse
     */
    @Transactional 
    public EquipmentResponse updateEquipment(Long equipmentId, EquipmentRequest req){
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND","ไม่เจอ "+equipmentId  ));
       

        if (equipment.getStatus() != EquipmentStatus.AVAILABLE && equipment.getStatus() != EquipmentStatus.MAINTENANCE) {
            throw new ApiException(HttpStatus.CONFLICT, "EQUIPMENT_IN_USE", "อุปกรณ์นี้ถูกยืมอยู่ " +equipmentId);
        }
   
      
        Resource resource = null;
        if (req.resourceId() != null) {
            resource = resourceRepository.findById(req.resourceId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND","ไม่เจอ "+req.resourceId()));
        }
        EquipmentType equipmentType = equipmentTypeRepository.findById(req.equipmentTypeId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EQUIPMENT_TYPE_NOT_FOUND","ไม่เจอ "+req.equipmentTypeId()+" ของ EQUIPMENTTYPE "));
        
        equipment.update(equipmentType, req.barcode().trim(), resource);
        if (req.status() != null) {     
                 
            if (req.status() != EquipmentStatus.AVAILABLE && req.status() != EquipmentStatus.MAINTENANCE) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                        "เปลี่ยนสถานะได้เฉพาะ AVAILABLE หรือ MAINTENANCE");
            }                                  
                equipment.changeStatus(req.status());
        }
      

        return EquipmentResponse.from(equipment) ;
    }

    /**
     * เอาไว้ลบ Equipment  method นี้เป็นแบบ Trasactionnal
     * มีการค้นหาว่า Equipment ถูกยืมอยู่ไหมถ้าถูกยืมแล้วจะไม่ให้ลบ
     * และโดยมีการค้นหา equipmentId ก่อนที่จะทำการ Delete Equipment
     * มีการเช็คว่า  Equipment มีประวัติใน loan ไหม
     * @param equipmentId id ของ Equipment ที่จะไป update
     */
    @Transactional 
    public void deleteEquipment(Long equipmentId){
        Equipment equipment = equipmentRepository.findById(equipmentId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EQUIPMENT_NOT_FOUND","ไม่เจอ "+equipmentId  ));
        if (equipment.getStatus() == EquipmentStatus.BORROWED) {
            throw new ApiException(HttpStatus.CONFLICT, "EQUIPMENT_IN_USE", "อุปกรณ์นี้ถูกยืมอยู่ " +equipmentId);
        }
        
        if (loanRepository.existsByEquipment(equipment)) {
            throw new ApiException(HttpStatus.CONFLICT, "EQUIPMENT_HAS_HISTORY", "อุปกรณ์มีประวัติในการยืม " +equipmentId);
        }
        equipmentRepository.delete(equipment);

    }
}
