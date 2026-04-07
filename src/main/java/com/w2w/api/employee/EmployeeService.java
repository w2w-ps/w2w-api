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

    public Employee saveEmployee(
            Integer employeeId,
            String status,
            java.time.LocalDateTime lastLogon,
            Integer logonCount,
            String firstName,
            String lastName,
            String employeeNumber,
            String email,
            java.util.List<String> phones,
            java.time.LocalDateTime hireDate,
            Integer maxScheduledHours,
            Integer maxDailyHours,
            Float payRate
    ) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setCompanyId(CurrentTenant.requireCurrentTenant());
        employee.setStatus(status);
        employee.setLastLogon(lastLogon);
        employee.setLogonCount(logonCount);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmployeeNumber(employeeNumber);
        employee.setEmail(email);
        employee.setPhones(phones);
        employee.setHireDate(hireDate);
        employee.setMaxScheduledHours(maxScheduledHours);
        employee.setMaxDailyHours(maxDailyHours);
        employee.setPayRate(payRate);
        return employeeRepository.save(employee);
    }

    public void deleteEmployee(Integer id) {
        Employee employee = employeeRepository.findByEmployeeIdAndCompanyId(id, CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        employeeRepository.delete(employee);
    }
}
