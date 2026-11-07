package com.cosync.identity.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cosync.identity.dto.employee.ChangePasswordRequest;
import com.cosync.identity.dto.login.LoginRequest;
import com.cosync.identity.dto.login.LoginResult;
import com.cosync.identity.dto.login.MeResponse;
import com.cosync.identity.dto.login.StaffLoginResult;
import com.cosync.identity.dto.login.StaffMeResponse;
import com.cosync.identity.dto.otp.ResendOtpRequest;
import com.cosync.identity.dto.otp.VerifyOtpRequest;
import com.cosync.identity.dto.register.RegisterRequest;
import com.cosync.identity.dto.register.RegisterResponse;
import com.cosync.identity.security.JwtService;
import com.cosync.identity.security.StaffOnly;
import com.cosync.identity.service.AuthService;
import com.cosync.identity.service.EmployeeService;

import jakarta.validation.Valid;


@RestController 
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService; 
    private final EmployeeService employeeService;

    public AuthController(AuthService authService,JwtService jwtService,EmployeeService employeeService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.employeeService = employeeService;
    }
    
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }
    
    @PostMapping("/verify-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
    }

    @PostMapping("/login")
    public ResponseEntity<MeResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.createCookie(result.token()).toString())
                .body(result.me());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, jwtService.clearCookie().toString())
                .build();
    }

    @PostMapping("/resend-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendOtp(request);
    }


    @PostMapping("/staff/login")
    public ResponseEntity<StaffMeResponse> staffLogin(@Valid @RequestBody LoginRequest request) {
        StaffLoginResult result = authService.staffLogin(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtService.createCookie(result.token()).toString())
                .body(result.me());
    }

    @PutMapping("/staff/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @StaffOnly
    public void changeStaffPassword(
            @RequestHeader("X-User-Id") Long employeeId,
            @Valid @RequestBody ChangePasswordRequest request) {
        employeeService.changePassword(employeeId, request);
    }
}
