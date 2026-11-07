package com.cosync.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.identity.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByName(String name);;
    boolean existsByNameAndIdNot(String name, Long id);
}
