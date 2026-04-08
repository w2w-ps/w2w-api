package com.w2w.api.employee;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmployeeListConfigService {

    @Autowired
    private EmployeeListConfigRepository configRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private List<String> getAvailableColumns() {
        // Dynamically discover columns from the tables you specified
        String sql = "SELECT column_name FROM information_schema.columns " +
                     "WHERE table_name IN ('employee', 'employee_address') " +
                     "AND table_schema = 'public' " +
                     "AND column_name NOT IN ('employee_id', 'company_id', 'emp_type_id')";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<EmployeeListConfig> getConfigsByCompany(Integer companyId) {
        List<EmployeeListConfig> existingConfigs = configRepository.findByCompanyId(companyId);
        Map<String, EmployeeListConfig> configMap = existingConfigs.stream()
                .collect(Collectors.toMap(EmployeeListConfig::getColumnName, c -> c));

        return getAvailableColumns().stream().map(columnName -> {
            if (configMap.containsKey(columnName)) {
                return configMap.get(columnName);
            }
            EmployeeListConfig config = new EmployeeListConfig();
            config.setCompanyId(companyId);
            config.setColumnName(columnName);
            config.setIsVisible(true); // Default to true if never set
            return config;
        }).collect(Collectors.toList());
    }

    @Transactional
    public List<EmployeeListConfig> saveConfigs(Integer companyId, Map<String, Boolean> columnVisibilities) {
        List<String> availableColumns = getAvailableColumns();
        
        availableColumns.forEach(columnName -> {
            EmployeeListConfig config = configRepository
                    .findByCompanyIdAndColumnName(companyId, columnName)
                    .orElse(new EmployeeListConfig());
            config.setCompanyId(companyId);
            config.setColumnName(columnName);
            
            // Authorized update: set to false if missing from request
            config.setIsVisible(columnVisibilities.getOrDefault(columnName, false));
            
            configRepository.save(config);
        });
        
        return getConfigsByCompany(companyId);
    }
}
