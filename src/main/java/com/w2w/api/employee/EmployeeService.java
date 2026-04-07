package com.w2w.api.employee;

import com.w2w.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getEmployeesByCompany(Integer companyId) {
        return employeeRepository.findByCompanyId(TenantContext.resolveTenant(companyId));
    }

    public Optional<Employee> getEmployeeById(Integer id, Integer companyId) {
        Integer resolvedCompanyId = TenantContext.resolveTenant(companyId);
        return resolvedCompanyId != null
                ? employeeRepository.findByEmployeeIdAndCompanyId(id, resolvedCompanyId)
                : employeeRepository.findById(id);
    }

    public Employee saveEmployee(Employee employee) {
        employee.setCompanyId(TenantContext.resolveTenant(employee.getCompanyId()));
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Integer id) {
        employeeRepository.deleteById(id);
    }
}
