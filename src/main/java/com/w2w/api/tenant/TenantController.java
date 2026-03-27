package com.w2w.api.tenant;

import com.w2w.api.config.TenantContext;
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
        return tenantService.saveCompany(company);
    }

    @GetMapping("/{id}")
    public Company getById(@PathVariable Integer id) {
        TenantContext.setCurrentTenant(id);
        return tenantService.getCompanyById(id).orElse(null);
    }
}
