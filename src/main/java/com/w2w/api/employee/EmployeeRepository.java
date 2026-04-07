package com.w2w.api.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    List<Employee> findByCompanyId(Integer companyId);
    Optional<Employee> findByEmployeeIdAndCompanyId(Integer employeeId, Integer companyId);
}
