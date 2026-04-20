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

    Optional<Employee> findByCompanyIdAndEmailAndIsDeletedFalse(Integer companyId, String email);

    @org.springframework.data.jpa.repository.Query(value = """
                SELECT DISTINCT e.* FROM employee e
                JOIN employee_position ep ON e.employee_id = ep.employee_id
                WHERE ep.position_id IN :positionIds AND e.company_id = :companyId
            """, nativeQuery = true)
    List<Employee> findByPositionIdsAndCompanyId(List<Integer> positionIds, Integer companyId);
}
