package com.cosync.wallet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.cosync.wallet.security.InternalTokenInterceptor;
import com.cosync.wallet.security.PermissionInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;
    private final InternalTokenInterceptor internalTokenInterceptor;
    public WebConfig(PermissionInterceptor permissionInterceptor,InternalTokenInterceptor internalTokenInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
        this.internalTokenInterceptor = internalTokenInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor);
        registry.addInterceptor(internalTokenInterceptor).addPathPatterns("/internal/**");
    }
}