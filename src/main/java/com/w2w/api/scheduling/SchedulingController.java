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
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
        schedulingService.saveShift(
                request.employeeId(),
                request.description(),
                request.date(),
                request.startTime(),
                request.endTime(),
                request.duration(),
                request.position(),
                request.category(),
                request.color()
        );
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
        return schedulingService.getEmployeeShiftsGroupedInRange(startDate, endDate);
    }

    @GetMapping("/shifts/grouped")
    public Object getGroupedShifts(
            @RequestParam Integer companyId,
            @RequestParam ShiftGrouping grouping,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGrouped(startDate, endDate, grouping);
    }

    @GetMapping("/shifts/date-position")
    public List<DayPositionBucket> getShiftsGroupedByDateAndPosition(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGroupedByDateAndPosition(startDate, endDate);
    }

    @GetMapping("/shifts/day-position-timing")
    public List<DayPositionTimingBucketDto> getShiftsGroupedByDayPositionAndTiming(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGroupedByDayPositionAndTiming(startDate, endDate);
    }

    @PostMapping("/validation/precheck")
    public ResponseEntity<ConflictResponse> preCheck(@RequestBody FindConflictRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        List<ConflictItem> conflicts = schedulingService.validate(request);
        return ResponseEntity.ok(new ConflictResponse(!conflicts.isEmpty(), conflicts));
    }
}
