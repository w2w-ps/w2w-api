package com.w2w.api.employee;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    public List<Employee> getEmployeesByCompany() {
        return employeeRepository.findByCompanyId(TenantContext.getCurrentTenant());
    }

    public Optional<Employee> getEmployeeById(Integer id) {
        return employeeRepository.findByEmployeeIdAndCompanyId(id, TenantContext.getCurrentTenant());
    }

    public Employee saveEmployee(Employee employee) {
        employee.setCompanyId(CurrentTenant.requireCurrentTenant());
        
        if (employee.getAddress() != null) {
            employee.getAddress().setEmployee(employee);
        }
        
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Integer id) {
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyId(id, CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        employeeRepository.delete(employee);
    }
}
