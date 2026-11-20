package com.cosync.booking.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.booking.common.ApiException;
import com.cosync.booking.dto.rate.RateResponse;
import com.cosync.booking.dto.rate.RateUpdateRequest;
import com.cosync.booking.entity.ServiceRate;
import com.cosync.booking.entity.enumtype.ServiceType;
import com.cosync.booking.repository.ServiceRateRepository;

@Service 
public class RateService {
    

  
    private final ServiceRateRepository serviceRateRepository;

    public RateService(ServiceRateRepository serviceRateRepository){
        this.serviceRateRepository = serviceRateRepository;
    }

    /**
     * ดึงข้อมูล rate จาก serviceRate ออกมาเป็น list ส
     * @return ส่งกลับเป็นเป็น format RateResponse
     */
    public List<RateResponse> list(){
            List<ServiceRate> rates = serviceRateRepository.findAll();
        return rates.stream().map(RateResponse::from).toList();
    }

    /**
     * update ค่าใน RateReponse 
     * มีการค้นหา  serviceType ว่ามีใน ServiceRate ไหม
     * @param staffId id ของ staff
     * @param serviceType ประเภทของ service
     * @param req คำขอการ update ข้อมูลในรูปแบบ RateUpdateRequest
     * @return ส่งกลับแบบ RateResponse
     */
    @Transactional 
    public RateResponse update(Long staffId,ServiceType serviceType,RateUpdateRequest req){
        ServiceRate rate = serviceRateRepository.findById(serviceType).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RATE_NOT_FOUND","ไม่พบราคาของ " + serviceType));
        rate.update(req.rate(), req.discountPct(), "STF-%03d".formatted(staffId));
        serviceRateRepository.flush();
        
        return RateResponse.from(rate);
    }
}

