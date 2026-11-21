package com.cosync.booking.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.equipment.CreateEquipmentTypeRequest;
import com.cosync.booking.dto.equipment.EquipmentPricingRequest;
import com.cosync.booking.dto.equipment.EquipmentTypeResponse;
import com.cosync.booking.entity.EquipmentType;
import com.cosync.booking.repository.EquipmentTypeRepository;

@Service 
public class EquipmentTypeService {
    private final EquipmentTypeRepository equipmentTypeRepository;

    public EquipmentTypeService(EquipmentTypeRepository equipmentTypeRepository){
        this.equipmentTypeRepository = equipmentTypeRepository;
    }

    /**
     * ดึงข้อมูล EquipmentType ทั้งหมดออกมาเป็น List
     * @return ตอนกลับแบบ EquipmentTypeResponse 
     */
    public List<EquipmentTypeResponse> listEquipmentType(){
        List<EquipmentType> equipments = equipmentTypeRepository.findAll();
        return equipments.stream().map(EquipmentTypeResponse::from).toList();
    }

    /**
     * สร้างรายการ  EquipmentType ใหม่ โดย method นี้เป็นแบบ Transaction 
     * @param req คำขอในการสร้าง EquipmentType ใหม่่
     * @return ส่งกลับแบบ EquipmentTypeResponse
     */
    @Transactional
    public EquipmentTypeResponse createEquipmentType(CreateEquipmentTypeRequest req){
      
        EquipmentType equipment = new EquipmentType(req.name().trim(), req.isLoanable());
        equipmentTypeRepository.save(equipment);
        return  EquipmentTypeResponse.from(equipment);
    }

    /**
     * เอาไว้ตั้งค่่า Pricing ของ equipmentType ใหม่ โดย method นี้เป็นแบบ Transaction 
     * โดยจะมีการเช็คว่าเคยมี  equipmentType id นี้แล้วหรือยัง
     * @param staffId id ของพนักงาน
     * @param id id ของ equipmentType
     * @param req คำขอในการ setPricing
     * @return ตอบกลับแบบ EquipmentTypeResponse
     */
    @Transactional
    public  EquipmentTypeResponse setPricing(Long staffId,Long id,EquipmentPricingRequest req){
        EquipmentType equipmentType = equipmentTypeRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EQUIPMENT_TYPE_NOT_FOUND","ไม่เจอ "+id+" ของ EQUIPMENTTYPE "));
        if (!equipmentType.isLoanable()) {                                                    
            throw new ApiException(HttpStatus.BAD_REQUEST, "EQUIPMENT_TYPE_NOT_LOANABLE",
                    "อุปกรณ์ประเภทนี้ไม่ได้ให้ยืม จึงตั้งค่ายืมไม่ได้");
        }
        equipmentType.setPricing(req.loanFee(), req.deposit(), "STF-%03d".formatted(staffId));
        equipmentTypeRepository.save(equipmentType);
        return EquipmentTypeResponse.from(equipmentType);
    }
}
