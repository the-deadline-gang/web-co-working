package com.cosync.identity.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cosync.identity.common.ApiException;
import com.cosync.identity.dto.role.PermissionResponse;
import com.cosync.identity.dto.role.RoleRequest;
import com.cosync.identity.dto.role.RoleResponse;
import com.cosync.identity.entity.Permission;
import com.cosync.identity.entity.Role;
import com.cosync.identity.repository.PermissionRepository;
import com.cosync.identity.repository.RoleRepository;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleService(RoleRepository roleRepository,PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)   // ต้องมี เพราะ getPermissionCodes() โหลดแบบ LAZY
    public List<RoleResponse> listRoles() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissionRepository.findAll(Sort.by("id")).stream()
                .map(PermissionResponse::from)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        String name = request.name().trim();
    
        if (roleRepository.existsByName(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "ROLE_NAME_TAKEN", "ชื่อ role นี้ถูกใช้แล้ว");
        }
    
        Set<Permission> permissions = findPermissions(request.permissions());
    
        Role role = new Role(name);
        role.replacePermissions(permissions);
        roleRepository.save(role);
    
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse update(Long roleId, RoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "ไม่พบ role ที่ระบุ"));
    
        String name = request.name().trim();
        if (roleRepository.existsByNameAndIdNot(name, roleId)) {
            throw new ApiException(HttpStatus.CONFLICT, "ROLE_NAME_TAKEN", "ชื่อ role นี้ถูกใช้แล้ว");
        }
    
        Set<Permission> permissions = findPermissions(request.permissions());
    
        role.rename(name);
        role.replacePermissions(permissions);
    
        return RoleResponse.from(role);
    }

    /** แปลง code ที่ frontend ส่งมาเป็น Permission ถ้ามีตัวไหนไม่มีอยู่จริง → 422 */
    private Set<Permission> findPermissions(List<String> codes) {
        Set<String> uniqueCodes = new HashSet<>(codes);   // ตัดตัวซ้ำออกก่อน
        List<Permission> found = permissionRepository.findByCodeIn(uniqueCodes);
        if (found.size() != uniqueCodes.size()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "PERMISSION_NOT_FOUND", "มีสิทธิ์ที่ไม่มีอยู่ในระบบ");
        }
        return new HashSet<>(found);
    }
}