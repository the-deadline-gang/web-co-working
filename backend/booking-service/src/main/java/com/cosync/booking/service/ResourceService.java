package com.cosync.booking.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.equipment.EquipmentResponse;
import com.cosync.booking.dto.resource.ResourceRequest;
import com.cosync.booking.dto.resource.ResourceResponse;
import com.cosync.booking.entity.Resource;
import com.cosync.booking.entity.Zone;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.repository.EquipmentRepository;
import com.cosync.booking.repository.ResourceRepository;
import com.cosync.booking.repository.ZoneRepository;



@Service  
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final ZoneRepository zoneRepository;
    private final EquipmentRepository equipmentRepository;
    public ResourceService(ResourceRepository resourceRepository,ZoneRepository zoneRepository,EquipmentRepository equipmentRepository){
        this.resourceRepository = resourceRepository;
        this.zoneRepository = zoneRepository;
        this.equipmentRepository =equipmentRepository;
    }

    /**
     * ส่ง list ของทรัพยากรของห้องนั้น
     * โดยจะมีการกรองว่าจะหาเฉพาะ service กับ zoneId หรือจะหาแบบไม่กรองอะไรเลย
     * @param serviceType ประเภทของบริการ
     * @param zoneId id ของ zone
     * @return ส่งกลับแบบ ResourceResponse
     */
    @Transactional(readOnly = true)
    public List<ResourceResponse> listResource(ServiceType serviceType,Long zoneId){
        return  resourceRepository.findAll().stream()
                .filter(r -> serviceType == null || r.getServiceType() == serviceType)
                .filter(r -> zoneId == null || r.getZone().getId().equals(zoneId))
                .map(r -> ResourceResponse.from(r, fixedEquipmentOf(r)))
                .toList();
    }


    /**
     * สร้าง Resource ของห้องนั้น มี  Transactional
     * โดยจะมีการเช็คว่ามี zoneid นั้นมีจริงไหม แล้วก็จะเช็คค่า dailyQuota กับ capacity ว่ามันน้อยกว่า 0 ไหม
     * และก็จะมีการเลือกการสร้่างตามแบบ Day_pass กับ การจองปกติ
     * @param req คำขอในการสร้างในรูปแบบ ResourceRequest 
     * @return ส่งกลับแบบ  ResourceResponse ที่ถ้าไม่มีอุปกรณ์ไรจะส่ง list ว่างกลับไปให้
     */
    @Transactional 
    public ResourceResponse creatResource(ResourceRequest req){
        checkQuota(req);
        Zone zone =zoneRepository.findById(req.zoneId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND","ไม่เจอเลขของโซนนี้ " ));
        Resource resource;
    
        if (req.serviceType() == ServiceType.DAY_PASS) {
            resource = Resource.dayPass(zone,req.code().trim(),req.capacity(),req.dailyQuota(),req.sizeSqm(),req.imageUrl() );
        } else {
            resource = Resource.bookable(zone,req.code().trim(),req.serviceType(),req.capacity(),req.sizeSqm(),req.imageUrl());
        }
        resourceRepository.save(resource);
        return ResourceResponse.from(resource, fixedEquipmentOf(resource));
    }

    /**
     * เอาไว้ update ค่าใน Resource นั้น โดยมี Transactional
     * โดยจะมีการเช็คว่ามี zoneid นั้นมีจริงไหม แล้วก็จะเช็คค่า dailyQuota กับ capacity ว่ามันน้อยกว่า 0 ไหม
     * แล้วก็ทำการ save
     * @param resourceId id ของ resource นั้น
     * @param req คำขอในการสร้างในรูปแบบ ResourceRequest 
     * @return ส่งกลับแบบ  ResourceResponse ที่ถ้าไม่มีอุปกรณ์ไรจะส่ง list ว่างกลับไปให้
     */
    @Transactional 
    public ResourceResponse update(Long resourceId,ResourceRequest req){
        checkQuota(req);
        Resource resource = resourceRepository.findById(resourceId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND","ไม่เจอ Resource  "+ resourceId ));
        Zone zone = zoneRepository.findById(req.zoneId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ZONE_NOT_FOUND","ไม่เจอเลขของโซนนี้ " ));
        boolean active = req.isActive() != null ? req.isActive() : resource.isActive();
        resource.update(zone, req.code().trim(), req.serviceType(), req.capacity(), req.dailyQuota(), req.sizeSqm(), req.imageUrl(), active);

        return ResourceResponse.from(resource, fixedEquipmentOf(resource));
    }

    /**
     * เอาไว้เช็คว่า Quota ที่มีเป็นของ daypass นั้นมีไหมแล้ว
     * ถ้าเป็นแบบห้องหรือโต๊ะก็ไม่ควรจะมี Quota
     * @param req คำขอในรูปแบบ ResourceRequest 
     */
    private static void checkQuota(ResourceRequest req) {
        boolean isDayPass = req.serviceType() == ServiceType.DAY_PASS;
        boolean hasQuota  = req.dailyQuota() != null;
    
        if (isDayPass && !hasQuota) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Day Pass ต้องระบุโควตารายวัน");
        }
        if (!isDayPass && hasQuota) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "ห้องหรือโต๊ะไม่มีโควตารายวัน");
        }
    }


    /**
     * เอาไว้คนหาอุปกรณ์ในห้องนั้น
     * @param r รับเข้ามาเป็น Entity Resource
     * @return ส่งกลับแบบ EquipmentResponse ที่เป็น list
     */
    private List<EquipmentResponse> fixedEquipmentOf(Resource r) {
        return equipmentRepository.findByResource(r).stream().map(EquipmentResponse::from).toList();
    }
  
}
