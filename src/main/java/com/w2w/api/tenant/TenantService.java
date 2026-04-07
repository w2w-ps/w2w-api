package com.w2w.api.tenant;

import com.w2w.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TenantService {
    @Autowired
    private CompanyRepository companyRepository;

    public List<Company> getAllCompanies() {
        Integer currentTenant = TenantContext.getCurrentTenant();
        return currentTenant != null
                ? companyRepository.findById(currentTenant).stream().toList()
                : companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(Integer id) {
        return companyRepository.findById(TenantContext.resolveTenant(id));
    }

    public Company saveCompany(Company company) {
        company.setCompanyId(TenantContext.resolveTenant(company.getCompanyId()));
        return companyRepository.save(company);
    }

    public void deleteCompany(Integer id) {
        companyRepository.deleteById(id);
    }
}
