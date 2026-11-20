package com.cosync.booking.dto.rate;

import java.math.BigDecimal;

import com.cosync.booking.entity.ServiceRate;
import com.cosync.booking.entity.enumtype.RateUnit;
import com.cosync.booking.entity.enumtype.ServiceType;

public record RateResponse(

    ServiceType serviceType,
    String name,
    BigDecimal rate,
    RateUnit unit,
    BigDecimal discountPct
)
{    
   public static RateResponse from(ServiceRate r){
        return new RateResponse(r.getServiceType(),
                                r.getName(),
                                r.getRate(),
                                r.getUnit(),
                                r.getDiscountPct()
        );
   }
}
