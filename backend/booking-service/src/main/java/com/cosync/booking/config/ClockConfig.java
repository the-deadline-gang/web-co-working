package com.cosync.booking.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * นาฬิกากลางของระบบ ทุก service รับ Clock ผ่าน constructor แทนการเรียก Instant.now()
 * ตอนรันจริงใช้เวลาจริง (UTC) ส่วนใน test ส่ง Clock.fixed(...) เข้าไปเพื่อหยุดเวลาไว้
 */
@Configuration
public class ClockConfig {

    @Bean 
    public Clock clock(){
        return Clock.systemUTC();
    }
}
