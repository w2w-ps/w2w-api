package com.w2w.api.employee;

import com.w2w.api.employee.dto.EmployeeDetailResponse;
import com.w2w.api.employee.dto.EmployeeListConfigResponse;
import com.w2w.api.employee.dto.EmployeeRequest;
import com.w2w.api.employee.dto.EmployeeResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping
    public List<EmployeeResponse> getByCompany() {
        return employeeService.getEmployeesByCompany();
    }


    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDetailResponse> getEmployeeDetail(@PathVariable Integer id) {
        return employeeService.getEmployeeDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody EmployeeRequest request) {
        employeeService.saveEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();

    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id, @Valid @RequestBody EmployeeRequest request) {
        employeeService.updateEmployee(id, request);
        return ResponseEntity.noContent().build();

    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> patch(@PathVariable Integer id, @RequestBody EmployeeRequest request) {
        employeeService.patchEmployee(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/config")
    public List<EmployeeListConfigResponse> getConfigs() {
        return configService.getConfigsByCompany();
    }

    @PatchMapping("/config")
    public List<EmployeeListConfigResponse> saveConfigs(
            @RequestBody Map<String, Boolean> columnVisibilities) {
        return configService.saveConfigs(columnVisibilities);
    }
}
