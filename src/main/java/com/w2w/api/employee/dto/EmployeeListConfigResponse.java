package com.w2w.api.employee.dto;

public record EmployeeListConfigResponse(
    Integer configId,
    Integer companyId,
    String columnName,
    Boolean isVisible
) {}
