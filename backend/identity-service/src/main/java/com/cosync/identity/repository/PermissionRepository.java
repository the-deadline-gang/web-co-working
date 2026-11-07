package com.cosync.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.identity.entity.Permission;

public interface  PermissionRepository extends JpaRepository<Permission, Long> {
    
}
