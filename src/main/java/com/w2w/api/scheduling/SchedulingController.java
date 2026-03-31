package com.w2w.api.scheduling;

import com.w2w.api.config.TenantContext;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {
    @Autowired
    private SchedulingService schedulingService;

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public void createShift(@Valid @RequestBody CreateShiftRequest request) {
        TenantContext.setCurrentTenant(request.getCompanyId());
        schedulingService.saveShift(request);
    }

    @PutMapping("/shifts/{transactionId}")
    public Shift updateShift(@PathVariable Integer transactionId, @RequestBody UpdateShiftRequest request) {
        // We might want to set tenant based on request or existing shift
        // For now, assume it's handled or we can fetch the shift first to get companyId
        return schedulingService.updateShift(transactionId, request);
    }

    @DeleteMapping("/shifts/{transactionId}")
    public void deleteShift(@PathVariable Integer transactionId) {
        schedulingService.softDeleteShift(transactionId);
    }

    @GetMapping("/employees")
    public List<EmployeeWithShiftsDto> getShiftsGroupedInRange(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getEmployeeShiftsGroupedInRange(companyId, startDate, endDate);
    }
}
