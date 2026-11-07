package com.cosync.identity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.security.MemberOnly;
import com.cosync.identity.service.AuthService;


@RestController
public class MeController {

   
    private final AuthService authService;

    public MeController(AuthService authService) {
         this.authService = authService;
    }
    @GetMapping("/me")
    @MemberOnly
    public MeResponse me(@RequestHeader("X-User-Id") Long userId) {
        return authService.getMe(userId);
    }
}