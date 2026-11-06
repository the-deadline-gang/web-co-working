package com.cosync.identity.service;

import java.security.SecureRandom;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.employee.CreateEmployeeRequest;
import com.cosync.identity.dto.employee.EmployeeResponse;
import com.cosync.identity.entity.Employee;
import com.cosync.identity.entity.Role;
import com.cosync.identity.mail.OtpMailer;
import com.cosync.identity.repository.EmployeeRepository;
import com.cosync.identity.repository.RoleRepository;


@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;   
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();  
    private final PasswordEncoder passwordEncoder;
    private final OtpMailer otpMailer;


    public EmployeeService(EmployeeRepository employeeRepository, RoleRepository roleRepository,PasswordEncoder passwordEncoder,OtpMailer otpMailer) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpMailer = otpMailer;
    }
    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
       
        String email = request.email().trim().toLowerCase();

        if (employeeRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "อีเมลนี้ถูกใช้แล้ว");
        }
        Role role = roleRepository.findById(request.roleId()).orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "ROLE_NOT_FOUND", "ไม่พบ role ที่ระบุ"));
       
        String tempPassword = generateTempPassword();
       
        Employee employee = new Employee(
                role,
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                request.phone(),
                passwordEncoder.encode(tempPassword)
            ); 
        employeeRepository.save(employee);

        otpMailer.sendWelcome(email, tempPassword);
       
        return EmployeeResponse.from(employee);
    }



    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
