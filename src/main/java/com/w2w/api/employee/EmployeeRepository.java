package com.w2w.api.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    List<Employee> findByCompanyId(Integer companyId);
    Optional<Employee> findByEmployeeIdAndCompanyId(Integer employeeId, Integer companyId);
    Optional<Employee> findByEmployeeNumber(String employeeNumber);

    @org.springframework.data.jpa.repository.Query(value = """
        SELECT DISTINCT e.* FROM employee e
        JOIN employee_position ep ON e.employee_id = ep.employee_id
        WHERE ep.position_id IN :positionIds AND e.company_id = :companyId
    """, nativeQuery = true)
    List<Employee> findByPositionIdsAndCompanyId(List<Integer> positionIds, Integer companyId);
}
