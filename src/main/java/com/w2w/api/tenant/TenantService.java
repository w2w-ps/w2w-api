package com.w2w.api.tenant;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class TenantService {
    private final CompanyRepository companyRepository;

    public TenantService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

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
            String tos,
            Integer weekStartDay
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
        company.setWeekStartDay(weekStartDay);
        return companyRepository.save(company);
    }

    public Company updateCompanySettings(Company updates) {
        Company existing = companyRepository.findById(CurrentTenant.requireCurrentTenant())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        if (updates.getCompanyName() != null) existing.setCompanyName(updates.getCompanyName());
        if (updates.getDepartmentName() != null) existing.setDepartmentName(updates.getDepartmentName());
        if (updates.getAddress() != null) existing.setAddress(updates.getAddress());
        if (updates.getCity() != null) existing.setCity(updates.getCity());
        if (updates.getState() != null) existing.setState(updates.getState());
        if (updates.getTimestamp() != null) existing.setTimestamp(updates.getTimestamp());
        if (updates.getTimezone() != null) existing.setTimezone(updates.getTimezone());
        if (updates.getStatus() != null) existing.setStatus(updates.getStatus());
        if (updates.getTrialStart() != null) existing.setTrialStart(updates.getTrialStart());
        if (updates.getDropDead() != null) existing.setDropDead(updates.getDropDead());
        if (updates.getPriceTable() != null) existing.setPriceTable(updates.getPriceTable());
        if (updates.getTos() != null) existing.setTos(updates.getTos());
        if (updates.getWeekStartDay() != null) existing.setWeekStartDay(updates.getWeekStartDay());

        return companyRepository.save(existing);
    }

    public void deleteCompany(Integer id) {
        companyRepository.deleteById(CurrentTenant.requireCurrentTenant());
    }
}
