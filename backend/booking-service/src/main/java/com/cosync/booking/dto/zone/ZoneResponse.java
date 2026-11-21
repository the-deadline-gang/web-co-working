package com.cosync.booking.dto.zone;

import com.cosync.booking.entity.Zone;

public record  ZoneResponse (
    Long zoneId,
    String name
)
{
    public static ZoneResponse from(Zone z){
        return  new ZoneResponse(z.getId(),
                                 z.getName()
        );
    }
    
}
