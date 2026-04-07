package com.w2w.api.tenant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    @Autowired
    private TenantService tenantService;

    @GetMapping
    public List<Company> getAll() {
        return tenantService.getAllCompanies();
    }

    @PostMapping
    public Company create(@RequestBody Company company) {
        return tenantService.saveCompany(
                company.getCompanyName(),
                company.getDepartmentName(),
                company.getAddress(),
                company.getCity(),
                company.getState(),
                company.getTimestamp(),
                company.getTimezone(),
                company.getStatus(),
                company.getTrialStart(),
                company.getDropDead(),
                company.getPriceTable(),
                company.getTos()
        );
    }

    @GetMapping("/{id}")
    public Company getById(@PathVariable Integer id) {
        return tenantService.getCompanyById(id).orElse(null);
    }
}
