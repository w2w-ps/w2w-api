package com.w2w.api.scheduling;

import com.w2w.api.config.TenantContext;
import com.w2w.api.scheduling.dto.ConflictDto;
import com.w2w.api.scheduling.dto.ConflictResponse;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.DayPositionBucketDto;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.model.Shift;
import jakarta.validation.Valid;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/scheduling")
public class SchedulingController {
    @Autowired
    private SchedulingService schedulingService;

    @GetMapping("/shifts/{shiftId}")
    public ShiftResponseDto getShift(@PathVariable Integer shiftId) {
        return schedulingService.getShift(shiftId);
    }

    @PostMapping("/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    public void createShift(@Valid @RequestBody CreateShiftRequest request) {
        TenantContext.setCurrentTenant(request.getCompanyId());
        schedulingService.saveShift(request);
    }

    @PutMapping("/shifts/{shiftId}")
    public Shift updateShift(@PathVariable Integer shiftId, @Valid @RequestBody UpdateShiftRequest request) {
        return schedulingService.updateShift(shiftId, request);
    }

    @DeleteMapping("/shifts/{shiftId}")
    public void deleteShift(@PathVariable Integer shiftId) {
        schedulingService.softDeleteShift(shiftId);
    }

    @GetMapping("/employees")
    public List<EmployeeWithShiftsDto> getShiftsGroupedInRange(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getEmployeeShiftsGroupedInRange(companyId, startDate, endDate);
    }

    @GetMapping("/shifts/range/grouped/day-position")
    public List<DayPositionBucketDto> getShiftsGroupedByDayAndPosition(
            @RequestParam Integer companyId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getShiftsGroupedByDayAndPosition(companyId, startDate, endDate);
    }

    @PostMapping("/validation/precheck")
    public ResponseEntity<ConflictResponse> preCheck(@RequestBody FindConflictRequest request) {
        List<ConflictDto> conflicts = schedulingService.validate(request);
        ConflictResponse response = new ConflictResponse(!conflicts.isEmpty(), conflicts);
        return ResponseEntity.ok(response);
    }
}
