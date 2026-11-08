package com.cosync.identity.service;

import static com.cosync.identity.testsupport.TestData.hash;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;

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
import com.cosync.identity.testsupport.TestData;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final long ADMIN_ID = 1L;
    private static final long COUNTER_ID = 2L;
    private static final Sort BY_ID = Sort.by("id");

    @Mock EmployeeRepository employeeRepository;
    @Mock RoleRepository roleRepository;
    @Mock MailService mailService;

    private EmployeeService employeeService;

    private Role adminRole;
    private Role managerRole;
    private Role counterRole;
    private Employee admin;
    private Employee counter;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, roleRepository, TestData.PASSWORD_ENCODER, mailService);

        adminRole = TestData.role(1L, "Admin", TestData.permission(1L, "role.manage"));
        managerRole = TestData.role(3L, "Manager", TestData.permission(4L, "resource.manage"));
        counterRole = TestData.role(5L, "Counter", TestData.permission(8L, "loan.record"));

        admin = TestData.employee(ADMIN_ID, adminRole, "admin@cosync.local", hash("Admin@1234"));
        counter = TestData.employee(COUNTER_ID, counterRole, "counter1@cosync.local", hash("Counter@1234"));
    }

    private static UpdateEmployeeRequest updateRequest(String email, long roleId, EmployeeStatus status) {
        return new UpdateEmployeeRequest("สมชาย", "ใจดี", email, null, roleId, status);
    }

    private static ApiException assertApiError(String code, HttpStatus status, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(code, e.getCode());
        assertEquals(status, e.getStatus());
        return e;
    }

    // =================== สร้าง ===================

    @Test
    @DisplayName("TC-EMP-01 สร้างพนักงาน: ACTIVE, รหัสชั่วคราว 12 ตัวส่งทางอีเมล, DB เก็บแค่ hash")
    void create_success() {
        when(employeeRepository.existsByEmail("counter9@cosync.local")).thenReturn(false);
        when(roleRepository.findById(5L)).thenReturn(Optional.of(counterRole));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> TestData.withId(inv.getArgument(0), 9L));

        EmployeeResponse response = employeeService.create(
                new CreateEmployeeRequest(" สมชาย ", "ใจดี", " Counter9@COSYNC.local ", null, 5L));

        assertEquals("STF-009", response.displayId());
        assertEquals("counter9@cosync.local", response.email());
        assertEquals("สมชาย", response.firstName());
        assertEquals("Counter", response.roleName());
        assertEquals("ACTIVE", response.status());

        ArgumentCaptor<String> tempPassword = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendWelcome(eq("counter9@cosync.local"), tempPassword.capture());
        assertTrue(tempPassword.getValue().matches("[A-HJ-NP-Za-km-z2-9]{12}"),
                "รหัสชั่วคราวต้องยาว 12 ตัว และไม่มีตัวที่หน้าตาคล้ายกัน แต่ได้ " + tempPassword.getValue());

        ArgumentCaptor<Employee> saved = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(saved.capture());
        assertNotEquals(tempPassword.getValue(), saved.getValue().getPasswordHash());
        assertTrue(TestData.PASSWORD_ENCODER.matches(tempPassword.getValue(), saved.getValue().getPasswordHash()));
    }

    @Test
    @DisplayName("TC-EMP-03 อีเมลซ้ำ → 409 EMAIL_TAKEN ไม่บันทึก ไม่ส่งอีเมล")
    void create_emailTaken() {
        when(employeeRepository.existsByEmail("counter1@cosync.local")).thenReturn(true);

        assertApiError("EMAIL_TAKEN", HttpStatus.CONFLICT, () -> employeeService.create(
                new CreateEmployeeRequest("a", "b", "counter1@cosync.local", null, 5L)));

        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(roleRepository, mailService);
    }

    @Test
    @DisplayName("TC-EMP-04 roleId ไม่มีอยู่จริง → 422 ROLE_NOT_FOUND")
    void create_roleNotFound() {
        when(employeeRepository.existsByEmail("x@cosync.local")).thenReturn(false);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertApiError("ROLE_NOT_FOUND", HttpStatus.UNPROCESSABLE_CONTENT, () -> employeeService.create(
                new CreateEmployeeRequest("a", "b", "x@cosync.local", null, 99L)));

        verify(employeeRepository, never()).save(any());
        verifyNoInteractions(mailService);
    }

    @Test
    @DisplayName("TC-EMP-06 ส่งอีเมลไม่ผ่าน → exception หลุดออกไป (ให้ @Transactional rollback ได้)")
    void create_mailFails_propagates() {
        when(employeeRepository.existsByEmail("x@cosync.local")).thenReturn(false);
        when(roleRepository.findById(5L)).thenReturn(Optional.of(counterRole));
        doThrow(new MailSendException("MailHog down")).when(mailService).sendWelcome(eq("x@cosync.local"), any());

        assertThrows(MailSendException.class, () -> employeeService.create(
                new CreateEmployeeRequest("a", "b", "x@cosync.local", null, 5L)));
    }

    // =================== รายชื่อ ===================

    @Test
    @DisplayName("TC-EMP-07 ไม่ใส่ status → ทั้งหมด เรียงตาม id")
    void list_all() {
        when(employeeRepository.findAll(BY_ID)).thenReturn(List.of(admin, counter));

        List<EmployeeResponse> result = employeeService.list(null);

        assertEquals(List.of("STF-001", "STF-002"), result.stream().map(EmployeeResponse::displayId).toList());
        verify(employeeRepository, never()).findByStatus(any(), any());
    }

    @Test
    @DisplayName("TC-EMP-08 status=SUSPENDED → ใช้ findByStatus")
    void list_byStatus() {
        counter.changeStatus(EmployeeStatus.SUSPENDED);
        when(employeeRepository.findByStatus(EmployeeStatus.SUSPENDED, BY_ID)).thenReturn(List.of(counter));

        List<EmployeeResponse> result = employeeService.list(EmployeeStatus.SUSPENDED);

        assertEquals(1, result.size());
        assertEquals("SUSPENDED", result.get(0).status());
    }

    // =================== แก้ไข ===================

    /** ตั้ง repository ให้หา counter เจอ อีเมลไม่ชน และหา role ปลายทางเจอ */
    private void givenUpdatableCounter(String email, Role targetRole) {
        when(employeeRepository.findById(COUNTER_ID)).thenReturn(Optional.of(counter));
        when(employeeRepository.existsByEmailAndIdNot(email, COUNTER_ID)).thenReturn(false);
        when(roleRepository.findById(targetRole.getId())).thenReturn(Optional.of(targetRole));
    }

    @Test
    @DisplayName("TC-EMP-10 เปลี่ยน role เป็น Manager")
    void update_changeRole() {
        givenUpdatableCounter("counter1@cosync.local", managerRole);

        EmployeeResponse response = employeeService.update(COUNTER_ID, ADMIN_ID,
                updateRequest("counter1@cosync.local", 3L, null));

        assertEquals("Manager", response.roleName());
        assertSame(managerRole, counter.getRole());
        verify(employeeRepository, never()).save(any());   // dirty checking บันทึกให้เอง
    }

    @Test
    @DisplayName("TC-EMP-11 ระงับพนักงาน → status SUSPENDED")
    void update_suspend() {
        givenUpdatableCounter("counter1@cosync.local", counterRole);

        employeeService.update(COUNTER_ID, ADMIN_ID,
                updateRequest("counter1@cosync.local", 5L, EmployeeStatus.SUSPENDED));

        assertFalse(counter.isActive());
    }

    @Test
    @DisplayName("TC-EMP-12 ไม่มีพนักงาน id นี้ → 404 EMPLOYEE_NOT_FOUND")
    void update_notFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertApiError("EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND, () -> employeeService.update(999L, ADMIN_ID,
                updateRequest("x@cosync.local", 5L, null)));
    }

    @Test
    @DisplayName("TC-EMP-13 Admin เปลี่ยน role ตัวเอง → 422 CANNOT_MODIFY_SELF")
    void update_selfRoleChange_rejected() {
        when(employeeRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertApiError("CANNOT_MODIFY_SELF", HttpStatus.UNPROCESSABLE_CONTENT, () -> employeeService.update(ADMIN_ID, ADMIN_ID,
                updateRequest("admin@cosync.local", 5L, null)));

        assertSame(adminRole, admin.getRole());
        verifyNoInteractions(roleRepository);
    }

    @Test
    @DisplayName("TC-EMP-14 Admin ระงับตัวเอง → 422 CANNOT_MODIFY_SELF")
    void update_selfSuspend_rejected() {
        when(employeeRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));

        assertApiError("CANNOT_MODIFY_SELF", HttpStatus.UNPROCESSABLE_CONTENT, () -> employeeService.update(ADMIN_ID, ADMIN_ID,
                updateRequest("admin@cosync.local", 1L, EmployeeStatus.SUSPENDED)));

        assertTrue(admin.isActive());
    }

    @Test
    @DisplayName("TC-EMP-15 Admin แก้ชื่อตัวเอง (role เดิม) → ได้")
    void update_selfRename_allowed() {
        when(employeeRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(employeeRepository.existsByEmailAndIdNot("admin@cosync.local", ADMIN_ID)).thenReturn(false);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(adminRole));

        EmployeeResponse response = employeeService.update(ADMIN_ID, ADMIN_ID,
                new UpdateEmployeeRequest(" Super ", "Admin", "admin@cosync.local", null, 1L, null));

        assertEquals("Super", response.firstName());
        assertTrue(admin.isActive());
    }

    @Test
    @DisplayName("TC-EMP-16 อีเมลชนกับพนักงานคนอื่น → 409 EMAIL_TAKEN")
    void update_emailTakenByOther() {
        when(employeeRepository.findById(COUNTER_ID)).thenReturn(Optional.of(counter));
        when(employeeRepository.existsByEmailAndIdNot("admin@cosync.local", COUNTER_ID)).thenReturn(true);

        assertApiError("EMAIL_TAKEN", HttpStatus.CONFLICT, () -> employeeService.update(COUNTER_ID, ADMIN_ID,
                updateRequest(" Admin@Cosync.local ", 5L, null)));

        assertEquals("counter1@cosync.local", counter.getEmail());
    }

    @Test
    @DisplayName("TC-EMP-17 ไม่ส่ง status → คงสถานะเดิม")
    void update_noStatus_keepsCurrent() {
        counter.changeStatus(EmployeeStatus.SUSPENDED);
        givenUpdatableCounter("counter1@cosync.local", counterRole);

        employeeService.update(COUNTER_ID, ADMIN_ID, updateRequest("counter1@cosync.local", 5L, null));

        assertEquals(EmployeeStatus.SUSPENDED, counter.getStatus());
    }

    // =================== เปลี่ยนรหัสผ่าน ===================

    @Test
    @DisplayName("TC-PWD-01 เปลี่ยนสำเร็จ → รหัสใหม่ใช้ได้ รหัสเก่าใช้ไม่ได้")
    void changePassword_success() {
        when(employeeRepository.findById(COUNTER_ID)).thenReturn(Optional.of(counter));

        employeeService.changePassword(COUNTER_ID, new ChangePasswordRequest("Counter@1234", "NewPass@5678"));

        assertTrue(TestData.PASSWORD_ENCODER.matches("NewPass@5678", counter.getPasswordHash()));
        assertFalse(TestData.PASSWORD_ENCODER.matches("Counter@1234", counter.getPasswordHash()));
    }

    @Test
    @DisplayName("TC-PWD-02 รหัสปัจจุบันผิด → 422 CURRENT_PASSWORD_INCORRECT รหัสไม่เปลี่ยน")
    void changePassword_wrongCurrent() {
        when(employeeRepository.findById(COUNTER_ID)).thenReturn(Optional.of(counter));
        String before = counter.getPasswordHash();

        assertApiError("CURRENT_PASSWORD_INCORRECT", HttpStatus.UNPROCESSABLE_CONTENT, () ->
                employeeService.changePassword(COUNTER_ID, new ChangePasswordRequest("wrong", "NewPass@5678")));

        assertEquals(before, counter.getPasswordHash());
    }

    @Test
    @DisplayName("TC-PWD-03 รหัสใหม่เหมือนเดิม → 422 SAME_PASSWORD")
    void changePassword_same() {
        when(employeeRepository.findById(COUNTER_ID)).thenReturn(Optional.of(counter));

        assertApiError("SAME_PASSWORD", HttpStatus.UNPROCESSABLE_CONTENT, () ->
                employeeService.changePassword(COUNTER_ID, new ChangePasswordRequest("Counter@1234", "Counter@1234")));
    }

    @Test
    @DisplayName("เปลี่ยนรหัสของพนักงานที่ไม่มีอยู่ → 404 EMPLOYEE_NOT_FOUND")
    void changePassword_notFound() {
        when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

        assertApiError("EMPLOYEE_NOT_FOUND", HttpStatus.NOT_FOUND, () ->
                employeeService.changePassword(999L, new ChangePasswordRequest("a", "NewPass@5678")));
    }
}
