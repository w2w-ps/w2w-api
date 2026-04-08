package com.w2w.api.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeListConfigRepository extends JpaRepository<EmployeeListConfig, Integer> {
    List<EmployeeListConfig> findByCompanyId(Integer companyId);
    Optional<EmployeeListConfig> findByCompanyIdAndColumnName(Integer companyId, String columnName);
}
