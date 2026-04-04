package com.w2w.api.scheduling;

import com.w2w.api.config.TenantContext;
import com.w2w.api.scheduling.dto.ConflictItem;
import com.w2w.api.scheduling.dto.ConflictResponse;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.DayPositionBucket;
import com.w2w.api.scheduling.dto.DayPositionTimingBucketDto;
import com.w2w.api.scheduling.dto.EmployeeSchedule;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.ShiftGrouping;
import com.w2w.api.scheduling.dto.ShiftResponse;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Shift;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {
    @Autowired
    private SchedulingService schedulingService;

    @GetMapping("/shifts/{shiftId}")
    public ShiftResponse getShift(@PathVariable Integer shiftId) {
        return schedulingService.getShift(shiftId);
    }

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public void createShift(@Valid @RequestBody CreateShiftRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        schedulingService.saveShift(request);
    }

    @PutMapping("/shifts/{shiftId}")
    public ShiftResponse updateShift(@PathVariable Integer shiftId, @Valid @RequestBody UpdateShiftRequest request) {
        return schedulingService.updateShift(shiftId, request);
    }

    @DeleteMapping("/shifts/{shiftId}")
    public void deleteShift(@PathVariable Integer shiftId) {
        schedulingService.softDeleteShift(shiftId);
    }

    @Deprecated(forRemoval = false)
    @GetMapping("/employees")
    public List<EmployeeSchedule> getShiftsGroupedInRange(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return getShiftEmployees(companyId, startDate, endDate);
    }

    @GetMapping("/shifts/employees")
    public List<EmployeeSchedule> getShiftEmployees(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getEmployeeShiftsGroupedInRange(companyId, startDate, endDate);
    }

    @GetMapping("/shifts/grouped")
    public Object getGroupedShifts(
            @RequestParam Integer companyId,
            @RequestParam ShiftGrouping grouping,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGrouped(companyId, startDate, endDate, grouping);
    }

    @GetMapping("/shifts/day-position")
    public List<DayPositionBucket> getShiftsGroupedByDayAndPosition(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGroupedByDayAndPosition(companyId, startDate, endDate);
    }

    @GetMapping("/shifts/day-position-timing")
    public List<DayPositionTimingBucketDto> getShiftsGroupedByDayPositionAndTiming(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGroupedByDayPositionAndTiming(companyId, startDate, endDate);
    }
    @PostMapping("/validation/precheck")
    public ResponseEntity<ConflictResponse> preCheck(@RequestBody FindConflictRequest request) {
        List<ConflictItem> conflicts = schedulingService.validate(request);
        return ResponseEntity.ok(new ConflictResponse(!conflicts.isEmpty(), conflicts));
    }
}
