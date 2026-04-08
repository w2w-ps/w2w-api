package com.w2w.api.employee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeListConfigService configService;

    @GetMapping("/company/{companyId}")
    public List<Employee> getByCompany(@PathVariable Integer companyId) {
        return employeeService.getEmployeesByCompany();
    }

    @GetMapping("/{id}")
    public Employee getById(@PathVariable Integer id, @RequestParam Integer companyId) {
        return employeeService.getEmployeeById(id).orElse(null);
    }

    @PostMapping
    public Employee create(@RequestBody Employee employee) {
        return employeeService.saveEmployee(employee);
    }

    @GetMapping("/config/{companyId}")
    public List<EmployeeListConfig> getConfigs(@PathVariable Integer companyId) {
        return configService.getConfigsByCompany(companyId);
    }

    @PatchMapping("/config/{companyId}")
    public List<EmployeeListConfig> saveConfigs(@PathVariable Integer companyId, @RequestBody Map<String, Boolean> columnVisibilities) {
        return configService.saveConfigs(companyId, columnVisibilities);
    }
}
