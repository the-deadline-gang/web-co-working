package com.cosync.booking.dto.resource;

import java.math.BigDecimal;
import java.util.List;

import com.cosync.booking.dto.equipment.EquipmentResponse;
import com.cosync.booking.entity.Resource;
import com.cosync.booking.entity.enumtype.ServiceType;

public record ResourceResponse(
    String code,
    Long zoneId,
    ServiceType serviceType,
    int capacity,
    Integer dailyQuota ,
    BigDecimal sizeSqm,
    String imageUrl,
    Boolean  isActive ,
    Long resourceId,
    String zoneName,
    List<EquipmentResponse> fixedEquipment
) {
    public static ResourceResponse from(Resource r, List<EquipmentResponse> fixedEquipment) { 
        return  new ResourceResponse(   r.getCode(),            
                                        r.getZone().getId(),    
                                        r.getServiceType(), 
                                        r.getCapacity(), 
                                        r.getDailyQuota() ,
                                        r.getSizeSqm(),r.getImageUrl() 
                                        , r.isActive(),
                                         r.getId(),               
                                        r.getZone().getName(),  
                                        fixedEquipment);        
     }
    
}
