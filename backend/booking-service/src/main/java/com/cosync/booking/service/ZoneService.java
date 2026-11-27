package com.cosync.booking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.booking.dto.zone.CreateZoneRequest;
import com.cosync.booking.dto.zone.ZoneResponse;
import com.cosync.booking.entity.Zone;
import com.cosync.booking.repository.ZoneRepository;

@Service 
public class ZoneService {
    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository){
        this.zoneRepository = zoneRepository;
    }

    /**
     * ดึงรายการ Zone ทั้งหมด
     * @return ส่งกลับเป็น list ในแบบ ZoneResponse
     */
    public List<ZoneResponse> listZones(){
        List<Zone> zone = zoneRepository.findAll();
        return  zone.stream().map(ZoneResponse::from).toList();
    }

    /**
     * สร้าง Zone ขึ้นมาใหม่ โดย function นี้จะมี Transaction 
     * @param req คำขอการสร้าง Zone ใหม่
     * @return ส่งกลับแบบ ZoneReponse
     */
    @Transactional
    public ZoneResponse createZone(CreateZoneRequest req){
        Zone zone = new Zone(req.name().trim());
        zoneRepository.save(zone);
        return ZoneResponse.from(zone);
    }
}
