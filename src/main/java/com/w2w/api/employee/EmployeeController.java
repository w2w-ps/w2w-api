package com.w2w.api.employee;

import com.w2w.api.employee.dto.EmployeeListConfigResponse;
import com.w2w.api.employee.dto.EmployeeRequest;
import com.w2w.api.employee.dto.EmployeeResponse;
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
    public List<EmployeeResponse> getByCompany(@PathVariable Integer companyId) {
        return employeeService.getEmployeesByCompany();
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Integer id, @RequestParam Integer companyId) {
        return employeeService.getEmployeeById(id).orElse(null);
    }

    @PostMapping
    public EmployeeResponse create(@RequestBody EmployeeRequest request) {
        return employeeService.saveEmployee(request);
    }

    @GetMapping("/config/{companyId}")
    public List<EmployeeListConfigResponse> getConfigs(@PathVariable Integer companyId) {
        return configService.getConfigsByCompany(companyId);
    }

    @PatchMapping("/config/{companyId}")
    public List<EmployeeListConfigResponse> saveConfigs(@PathVariable Integer companyId, @RequestBody Map<String, Boolean> columnVisibilities) {
        return configService.saveConfigs(companyId, columnVisibilities);
    }
}
