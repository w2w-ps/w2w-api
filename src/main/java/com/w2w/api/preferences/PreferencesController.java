package com.w2w.api.preferences;

import com.w2w.api.config.TenantContext;
import com.w2w.api.preferences.dto.DayPreferenceDto;
import com.w2w.api.preferences.dto.WeekPreferenceDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping("/day")
    public ResponseEntity<DayPreferenceDto> getDayPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        return preferencesService.getDayPreference(employeeId, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/week")
    public ResponseEntity<WeekPreferenceDto> getWeekPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        return preferencesService.getWeekPreference(employeeId, startDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/day")
    public void saveDayPreference(@Valid @RequestBody DayPreferenceDto dto) {
        TenantContext.setCurrentTenant(dto.getCompanyId());
        preferencesService.saveDayPreference(dto);
    }

    @PostMapping("/week")
    public void saveWeekPreference(@Valid @RequestBody WeekPreferenceDto dto) {
        TenantContext.setCurrentTenant(dto.getCompanyId());
        preferencesService.saveWeekPreference(dto);
    }
}
