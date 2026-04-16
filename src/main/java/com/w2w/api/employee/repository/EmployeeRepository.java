package com.w2w.api.employee.repository;

import com.w2w.api.employee.model.Employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    List<Employee> findByCompanyIdAndIsDeletedFalse(Integer companyId);
    Optional<Employee> findByEmployeeIdAndCompanyIdAndIsDeletedFalse(Integer employeeId, Integer companyId);
    
    // Kept for backward compatibility or internal lookups if needed
    List<Employee> findByCompanyId(Integer companyId);
    Optional<Employee> findByEmployeeIdAndCompanyId(Integer employeeId, Integer companyId);
}
