package com.cosync.identity.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.role.PermissionResponse;
import com.cosync.identity.dto.role.RoleRequest;
import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.entity.Permission;
import com.cosync.identity.entity.Role;
import com.cosync.identity.repository.PermissionRepository;
import com.cosync.identity.repository.RoleRepository;
import com.cosync.identity.testsupport.TestData;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock RoleRepository roleRepository;
    @Mock PermissionRepository permissionRepository;

    private RoleService roleService;

    private final Permission reportView = TestData.permission(7L, "report.view");
    private final Permission loanRecord = TestData.permission(8L, "loan.record");
    private final Permission checkoutOverride = TestData.permission(9L, "checkout.override");

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, permissionRepository);
    }

    private static ApiException assertApiError(String code, HttpStatus status, Runnable action) {
        ApiException e = assertThrows(ApiException.class, action::run);
        assertEquals(code, e.getCode());
        assertEquals(status, e.getStatus());
        return e;
    }

    // =================== อ่าน ===================

    @Test
    @DisplayName("TC-ROL-01 ดู role ทั้งหมดพร้อม code ของสิทธิ์")
    void listRoles() {
        when(roleRepository.findAll()).thenReturn(List.of(
                TestData.role(1L, "Admin", TestData.permission(1L, "role.manage")),
                TestData.role(5L, "Counter", loanRecord, checkoutOverride)));

        List<RoleResponse> roles = roleService.listRoles();

        assertEquals(2, roles.size());
        assertEquals(Set.of("loan.record", "checkout.override"), roles.get(1).permissions());
    }

    @Test
    @DisplayName("TC-ROL-02 ดูสิทธิ์ทั้งหมด อ่านจากตาราง permission เรียงตาม id")
    void listPermissions_sortedById() {
        when(permissionRepository.findAll(Sort.by("id"))).thenReturn(List.of(reportView, loanRecord));

        List<PermissionResponse> permissions = roleService.listPermissions();

        assertEquals(List.of("report.view", "loan.record"), permissions.stream().map(PermissionResponse::code).toList());
        verifyNoInteractions(roleRepository);   // ไม่ได้เดินผ่าน role
    }

    // =================== สร้าง ===================

    @Test
    @DisplayName("TC-ROL-03 สร้าง role พร้อม 3 สิทธิ์ (ตัดช่องว่างที่ชื่อ)")
    void create_success() {
        when(roleRepository.existsByName("Shift Lead")).thenReturn(false);
        when(permissionRepository.findByCodeIn(Set.of("loan.record", "checkout.override", "report.view")))
                .thenReturn(List.of(loanRecord, checkoutOverride, reportView));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> TestData.withId(inv.getArgument(0), 7L));

        RoleResponse response = roleService.create(new RoleRequest(" Shift Lead ",
                List.of("loan.record", "checkout.override", "report.view")));

        assertEquals(7L, response.roleId());
        assertEquals("Shift Lead", response.name());
        assertEquals(Set.of("loan.record", "checkout.override", "report.view"), response.permissions());
    }

    @Test
    @DisplayName("TC-ROL-04 ชื่อซ้ำ → 409 ROLE_NAME_TAKEN")
    void create_nameTaken() {
        when(roleRepository.existsByName("Admin")).thenReturn(true);

        assertApiError("ROLE_NAME_TAKEN", HttpStatus.CONFLICT,
                () -> roleService.create(new RoleRequest("Admin", List.of())));

        verify(roleRepository, never()).save(any());
        verifyNoInteractions(permissionRepository);
    }

    @Test
    @DisplayName("TC-ROL-05 มีสิทธิ์ที่ไม่มีอยู่จริง → 422 PERMISSION_NOT_FOUND ไม่สร้าง role")
    void create_unknownPermission() {
        when(roleRepository.existsByName("Hacker")).thenReturn(false);
        when(permissionRepository.findByCodeIn(Set.of("loan.record", "god.mode"))).thenReturn(List.of(loanRecord));

        assertApiError("PERMISSION_NOT_FOUND", HttpStatus.UNPROCESSABLE_CONTENT,
                () -> roleService.create(new RoleRequest("Hacker", List.of("loan.record", "god.mode"))));

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-ROL-06 code ซ้ำในรายการ → ตัดตัวซ้ำ ไม่ถือว่าผิด")
    void create_duplicateCodes() {
        when(roleRepository.existsByName("Viewer")).thenReturn(false);
        when(permissionRepository.findByCodeIn(Set.of("report.view"))).thenReturn(List.of(reportView));
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> TestData.withId(inv.getArgument(0), 8L));

        RoleResponse response = roleService.create(new RoleRequest("Viewer", List.of("report.view", "report.view")));

        assertEquals(Set.of("report.view"), response.permissions());
    }

    @Test
    @DisplayName("TC-ROL-08 permissions = [] → สร้าง role ที่ยังไม่มีสิทธิ์")
    void create_emptyPermissions() {
        when(roleRepository.existsByName("Trainee")).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> TestData.withId(inv.getArgument(0), 9L));

        RoleResponse response = roleService.create(new RoleRequest("Trainee", List.of()));

        assertEquals(Set.of(), response.permissions());
    }

    // =================== แก้ไข ===================

    @Test
    @DisplayName("TC-ROL-09 แก้สิทธิ์ ชื่อเดิม → เหลือเฉพาะสิทธิ์ใหม่ (ไม่นับชื่อตัวเองว่าซ้ำ)")
    void update_replacePermissions() {
        Role shiftLead = TestData.role(7L, "Shift Lead", loanRecord, checkoutOverride, reportView);
        when(roleRepository.findById(7L)).thenReturn(Optional.of(shiftLead));
        when(roleRepository.existsByNameAndIdNot("Shift Lead", 7L)).thenReturn(false);
        when(permissionRepository.findByCodeIn(Set.of("report.view"))).thenReturn(List.of(reportView));

        RoleResponse response = roleService.update(7L, new RoleRequest("Shift Lead", List.of("report.view")));

        assertEquals(Set.of("report.view"), response.permissions());
        assertEquals(1, shiftLead.getPermissions().size());
        verify(roleRepository, never()).existsByName(any());
        verify(roleRepository, never()).save(any());   // dirty checking
    }

    @Test
    @DisplayName("TC-ROL-10 เปลี่ยนชื่อไปชน role อื่น → 409 ชื่อเดิมไม่เปลี่ยน")
    void update_nameTaken() {
        Role shiftLead = TestData.role(7L, "Shift Lead", reportView);
        when(roleRepository.findById(7L)).thenReturn(Optional.of(shiftLead));
        when(roleRepository.existsByNameAndIdNot("Admin", 7L)).thenReturn(true);

        assertApiError("ROLE_NAME_TAKEN", HttpStatus.CONFLICT,
                () -> roleService.update(7L, new RoleRequest("Admin", List.of())));

        assertEquals("Shift Lead", shiftLead.getName());
    }

    @Test
    @DisplayName("TC-ROL-11 role ไม่มีอยู่จริง → 404 ROLE_NOT_FOUND")
    void update_notFound() {
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        assertApiError("ROLE_NOT_FOUND", HttpStatus.NOT_FOUND,
                () -> roleService.update(999L, new RoleRequest("X", List.of())));
    }
}
