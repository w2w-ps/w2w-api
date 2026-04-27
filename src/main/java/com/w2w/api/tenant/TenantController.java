package com.w2w.api.tenant;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

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
                company.getTos(),
                company.getWeekStartDay()
        );
    }

    @PutMapping
    public Company update(@RequestBody Company company) {
        return tenantService.updateCompanySettings(company);
    }

    @GetMapping("/{id}")
    public Company getById(@PathVariable Integer id) {
        return tenantService.getCompanyById(id).orElse(null);
    }
}
