package com.w2w.api.tenant;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
        return companyRepository.findById(TenantContext.getCurrentTenant());
    }

    public Company saveCompany(
            String companyName,
            String departmentName,
            String address,
            String city,
            String state,
            java.time.LocalDateTime timestamp,
            Float timezone,
            String status,
            java.time.LocalDate trialStart,
            java.time.LocalDate dropDead,
            Integer priceTable,
            String tos
    ) {
        Company company = new Company();
        company.setCompanyId(CurrentTenant.requireCurrentTenant());
        company.setCompanyName(companyName);
        company.setDepartmentName(departmentName);
        company.setAddress(address);
        company.setCity(city);
        company.setState(state);
        company.setTimestamp(timestamp);
        company.setTimezone(timezone);
        company.setStatus(status);
        company.setTrialStart(trialStart);
        company.setDropDead(dropDead);
        company.setPriceTable(priceTable);
        company.setTos(tos);
        return companyRepository.save(company);
    }

    public void deleteCompany(Integer id) {
        companyRepository.deleteById(CurrentTenant.requireCurrentTenant());
    }
}
