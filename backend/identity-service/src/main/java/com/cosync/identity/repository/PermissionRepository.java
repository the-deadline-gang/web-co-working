package com.cosync.identity.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.identity.entity.Permission;

public interface  PermissionRepository extends JpaRepository<Permission, Long> {
    List<Permission> findByCodeIn(Collection<String> codes);
}
