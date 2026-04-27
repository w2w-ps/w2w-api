package com.w2w.api.employee;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.employee.dto.EmployeeListConfigResponse;
import com.w2w.api.employee.model.EmployeeListConfig;
import com.w2w.api.employee.repository.EmployeeListConfigRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmployeeListConfigService {

    private final EmployeeListConfigRepository configRepository;
    private final JdbcTemplate jdbcTemplate;

    public EmployeeListConfigService(EmployeeListConfigRepository configRepository, JdbcTemplate jdbcTemplate) {
        this.configRepository = configRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private List<String> getAvailableColumns() {
        // Dynamically discover columns from the tables you specified
        String sql = "SELECT column_name FROM information_schema.columns " +
                     "WHERE table_name IN ('employee', 'employee_address') " +
                     "AND table_schema = 'public' " +
                     "AND column_name NOT IN ('employee_id', 'company_id', 'emp_type_id')";
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public List<EmployeeListConfigResponse> getConfigsByCompany() {
        Integer companyId = CurrentTenant.requireCurrentTenant();
        List<EmployeeListConfig> existingConfigs = configRepository.findByCompanyId(companyId);
        Map<String, EmployeeListConfig> configMap = existingConfigs.stream()
                .collect(Collectors.toMap(EmployeeListConfig::getColumnName, c -> c));

        return getAvailableColumns().stream().map(columnName -> {
            EmployeeListConfig config;
            if (configMap.containsKey(columnName)) {
                config = configMap.get(columnName);
            } else {
                config = new EmployeeListConfig();
                config.setCompanyId(companyId);
                config.setColumnName(columnName);
                config.setIsVisible(true); // Default to true if never set
            }
            return mapToResponse(config);
        }).toList();
    }

    @Transactional
    public List<EmployeeListConfigResponse> saveConfigs(Map<String, Boolean> columnVisibilities) {
        Integer companyId = CurrentTenant.requireCurrentTenant();
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
        
        return getConfigsByCompany();
    }

    private EmployeeListConfigResponse mapToResponse(EmployeeListConfig config) {
        return new EmployeeListConfigResponse(
            config.getConfigId(),
            config.getCompanyId(),
            config.getColumnName(),
            toDisplayName(config.getColumnName()),
            config.getIsVisible()
        );
    }

    private String toDisplayName(String columnName) {
        if (columnName == null || columnName.isEmpty()) return "";
        
        // 1. Replace underscores with spaces
        String withSpaces = columnName.replace('_', ' ');
        
        // 2. Capitalize each word
        StringBuilder displayName = new StringBuilder();
        String[] words = withSpaces.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                displayName.append(Character.toUpperCase(word.charAt(0)))
                           .append(word.substring(1).toLowerCase());
                if (i < words.length - 1) {
                    displayName.append(" ");
                }
            }
        }
        return displayName.toString();
    }
}
