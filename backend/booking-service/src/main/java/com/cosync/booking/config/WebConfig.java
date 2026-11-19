package com.cosync.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.cosync.booking.security.PermissionInterceptor;

/**
 * ลงทะเบียน interceptor
 * PermissionInterceptor ครอบทุก path, InternalTokenInterceptor ครอบเฉพาะ /internal/**
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;
   
    public WebConfig(PermissionInterceptor permissionInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
      
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor);
      
    }
}
