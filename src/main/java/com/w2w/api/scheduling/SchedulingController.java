package com.w2w.api.scheduling;

import com.w2w.api.config.TenantContext;
import com.w2w.api.scheduling.dto.ConflictDto;
import com.w2w.api.scheduling.dto.CreateShiftRequest;
import com.w2w.api.scheduling.dto.UpdateShiftRequest;
import com.w2w.api.scheduling.dto.ShiftResponseDto;
import com.w2w.api.scheduling.dto.EmployeeWithShiftsDto;
import com.w2w.api.scheduling.dto.FindConflictRequest;
import com.w2w.api.scheduling.dto.ConflictResponse; // Import the renamed DTO
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
        // TODO: Determine tenant context appropriately. It might be inferred from the shiftId, or the request.
        // The shiftId from the path variable is used here. If the request body also contains shiftId,
        // ensure consistency or decide which one takes precedence.
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
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        TenantContext.setCurrentTenant(companyId);
        return schedulingService.getEmployeeShiftsGroupedInRange(companyId, startDate, endDate);
    }

    /**
     * Performs pre-checks for create/update/reassign operations and returns any conflicts.
     *
     * @param request The validation request containing operation type and shift data (including IDs for reassignment/update).
     * @return A ConflictResponse containing a boolean indicating conflicts and a list of ConflictDto.
     */
    @PostMapping("/validation/precheck") // New endpoint path under /api/scheduling
    public ResponseEntity<ConflictResponse> preCheck(@RequestBody FindConflictRequest request) { // Use the renamed DTO and return type
        // TODO: Implement more complex validation rules by calling RuleEngineService methods
        // for specific operation types as needed. For now, basic checks are delegated.
        List<ConflictDto> conflicts = schedulingService.validate(request);

        boolean hasConflicts = !conflicts.isEmpty();
        ConflictResponse response = new ConflictResponse(hasConflicts, conflicts); // Use ConflictResponse

        return ResponseEntity.ok(response);
    }
}
