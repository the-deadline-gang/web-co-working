package com.cosync.identity.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.service.AuthService;


@RestController
public class MeController {

   
    private final AuthService authService;

    public MeController(AuthService authService) {
         this.authService = authService;
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader("X-User-Id") Long userId,
                         @RequestHeader("X-User-Type") String userType, 
                         @RequestHeader(value = "X-Permissions", required = false) String perms) {
        System.out.println("id=" + userId + " type=" + userType + " perms=" + perms);
        if (!"MEMBER".equals(userType)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "ไม่มีสิทธิ์เข้าถึง");
        }
      
        return  authService.getMe(userId);
    }
}