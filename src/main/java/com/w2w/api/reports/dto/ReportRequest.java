package com.w2w.api.reports.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record ReportRequest(
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String categoryFilter,
        String positionsFilter,
        String statusFilter,
        List<Map<String, Object>> data) {
}
