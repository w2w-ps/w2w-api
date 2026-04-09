package com.w2w.api.employee.repository;

import com.w2w.api.employee.model.Employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    List<Employee> findByCompanyIdAndStatusNot(Integer companyId, String status);
    Optional<Employee> findByEmployeeIdAndCompanyIdAndStatusNot(Integer employeeId, Integer companyId, String status);
    
    // Kept for backward compatibility or internal lookups if needed
    List<Employee> findByCompanyId(Integer companyId);
    Optional<Employee> findByEmployeeIdAndCompanyId(Integer employeeId, Integer companyId);
}
