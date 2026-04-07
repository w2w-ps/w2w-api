package com.w2w.api.employee;

import com.w2w.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @GetMapping("/company/{companyId}")
    public List<Employee> getByCompany(@PathVariable Integer companyId) {
        return employeeService.getEmployeesByCompany(companyId);
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Integer id, @RequestParam Integer companyId) {
        return employeeService.getEmployeeById(id, companyId).orElse(null);
    }

    @PostMapping
    public Employee create(@RequestBody Employee employee) {
        return employeeService.saveEmployee(employee);
    }
}
