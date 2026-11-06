package com.cosync.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** สิทธิ์แต่ละอย่าง โค้ดเช็กจาก code เช่น "role.manage" */
@Entity
@Table(name = "permission")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    protected Permission() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
}