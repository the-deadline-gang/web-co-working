package com.cosync.identity.service;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.employee.ChangePasswordRequest;
import com.cosync.identity.dto.employee.CreateEmployeeRequest;
import com.cosync.identity.dto.employee.EmployeeResponse;
import com.cosync.identity.dto.employee.UpdateEmployeeRequest;
import com.cosync.identity.entity.Employee;
import com.cosync.identity.entity.EmployeeStatus;
import com.cosync.identity.entity.Role;
import com.cosync.identity.mail.MailService;
import com.cosync.identity.repository.EmployeeRepository;
import com.cosync.identity.repository.RoleRepository;


@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;   
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();  
    private final PasswordEncoder passwordEncoder;
    private final MailService otpMailer;


    public EmployeeService(EmployeeRepository employeeRepository, RoleRepository roleRepository,PasswordEncoder passwordEncoder,MailService otpMailer) {
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


    @Transactional(readOnly = true)
    public List<EmployeeResponse> list(EmployeeStatus status) {
        Sort byId = Sort.by("id");

        List<Employee> employees = (status == null)
                ? employeeRepository.findAll(byId)
                : employeeRepository.findByStatus(status, byId);

        return employees.stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    /**
     *  Update โปรไฟล์พนักงาน โดยไม่สามารถแก้ไขตัวเองได้ หากมีการเปลี่ยนแปลงสถานะเป็น SUSPENDED หรือเปลี่ยน role
     * @param employeeId id ของพนักงานที่ต้องการแก้ไข
     * @param currentUserId id ของผู้ใช้งานปัจจุบัน
     * @param request ข้อมูลที่ต้องการแก้ไข
     * @return EmployeeResponse ข้อมูลพนักงานที่แก้ไขแล้ว
     */
    @Transactional
    public EmployeeResponse update(Long employeeId, Long currentUserId, UpdateEmployeeRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "ไม่พบพนักงานที่ระบุ"));

        
        boolean editingSelf = employeeId.equals(currentUserId);
        boolean roleChanged = !employee.getRole().getId().equals(request.roleId());
        boolean suspending = request.status() == EmployeeStatus.SUSPENDED;
        
        if (editingSelf && (roleChanged || suspending)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "CANNOT_MODIFY_SELF", "ไม่สามารถเปลี่ยน role หรือระงับบัญชีของตัวเองได้");
        }

        String email = request.email().trim().toLowerCase();
        if(employeeRepository.existsByEmailAndIdNot(email, employeeId)){
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "อีเมลนี้ถูกใช้แล้ว");
        }
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "ROLE_NOT_FOUND", "ไม่พบ role ที่ระบุ"));


        employee.updateProfile(role, request.firstName().trim(), request.lastName().trim(), email,request.phone());

        if (request.status() != null) {
            employee.changeStatus(request.status());
        }
        
        return EmployeeResponse.from(employee);
    }

    /**
     * เปลี่ยนรหัสผ่านของพนักงาน โดยต้องตรวจสอบรหัสผ่านปัจจุบันก่อน และรหัสผ่านใหม่ต้องไม่เหมือนรหัสผ่านปัจจุบัน
     * @param employeeId id ของพนักงานที่ต้องการเปลี่ยนรหัสผ่าน
     * @param request ข้อมูลรหัสผ่านปัจจุบันและรหัสผ่านใหม่
     */
    @Transactional
    public void changePassword(Long employeeId, ChangePasswordRequest request) {
  
        Employee employee = employeeRepository.findById(employeeId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND", "ไม่พบพนักงานที่ระบุ"));
        if (!passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "CURRENT_PASSWORD_INCORRECT", "รหัสผ่านปัจจุบันไม่ถูกต้อง");   
        }
        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "SAME_PASSWORD", "รหัสผ่านใหม่ต้องไม่เหมือนรหัสผ่านปัจจุบัน");
            
        }
        employee.changePassword(passwordEncoder.encode(request.newPassword()));
    }
    
}
