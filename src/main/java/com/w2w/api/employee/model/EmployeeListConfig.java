package com.w2w.api.employee.model;

import jakarta.persistence.*;

@Entity
@Table(name = "employee_list_config")
public class EmployeeListConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Integer configId;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "is_visible")
    private Boolean isVisible;

    public EmployeeListConfig() {}

    public Integer getConfigId() {
        return configId;
    }

    public void setConfigId(Integer configId) {
        this.configId = configId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }
}
