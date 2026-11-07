package com.cosync.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cosync.identity.entity.Employee;
import com.cosync.identity.entity.EmployeeStatus;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    boolean existsByEmail(String email);

    List<Employee> findByStatus(EmployeeStatus status, Sort sort);

    boolean existsByEmailAndIdNot(String email, Long id);
}