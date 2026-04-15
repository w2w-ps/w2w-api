package com.w2w.api.timeoff;

import com.w2w.api.config.TenantContext;
import com.w2w.api.timeoff.dto.CreateTimeOffRequest;
import com.w2w.api.timeoff.dto.TimeOffRequestSummary;
import com.w2w.api.timeoff.dto.TimeOffRequestsResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/time-off")
public class TimeOffController {

    private final TimeOffService timeOffService;

    public TimeOffController(TimeOffService timeOffService) {
        this.timeOffService = timeOffService;
    }

    @GetMapping("/requests")
    public ResponseEntity<TimeOffRequestsResponse> getTimeOffRequests(
            @RequestParam Integer companyId,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        try {
            return ResponseEntity.ok(timeOffService.getTimeOffRequests(companyId, employeeId, status, startDate, endDate));
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/requests")
    public ResponseEntity<TimeOffRequestSummary> createTimeOffRequest(@Valid @RequestBody CreateTimeOffRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        try {
            return ResponseEntity.status(201).body(timeOffService.createTimeOffRequest(request));
        } finally {
            TenantContext.clear();
        }
    }
}