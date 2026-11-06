package com.cosync.identity.entity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/** ตำแหน่งงาน เช่น Admin, Finance แต่ละตำแหน่งมีได้หลายสิทธิ์ */
@Entity
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 60)
    private String name;

    /** เชื่อมกับ Permission ผ่านตาราง role_permission (ไม่ต้องมี Entity ของตารางเชื่อม) */
    @ManyToMany
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    protected Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    /** code ของสิทธิ์ทั้งหมด เช่น ["role.manage", "employee.manage"] ไว้ใส่ใน JWT */
    public Set<String> getPermissionCodes() {
        return permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Set<Permission> getPermissions() { return permissions; }
}