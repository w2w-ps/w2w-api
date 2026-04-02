package com.w2w.api.preferences;

import com.w2w.api.config.TenantContext;
import com.w2w.api.preferences.dto.DayPreferenceDto;
import com.w2w.api.preferences.dto.DayPreferenceRepeatDto;
import com.w2w.api.preferences.dto.WeekPreferenceDto;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/day/range")
    public ResponseEntity<List<DayPreferenceDto>> getDayPreferencesRange(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        List<DayPreferenceDto> preferences = preferencesService.getDayPreferencesInRange(employeeId, startDate, endDate);
        return ResponseEntity.ok(preferences);
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

    @PostMapping("/day/repeat")
    public void saveDayPreferenceWithRepeat(@Valid @RequestBody DayPreferenceRepeatDto dto) {
        TenantContext.setCurrentTenant(dto.getCompanyId());
        preferencesService.saveDayPreferenceWithRepeat(dto);
    }

    @PostMapping("/week")
    public void saveWeekPreference(@Valid @RequestBody WeekPreferenceDto dto) {
        TenantContext.setCurrentTenant(dto.getCompanyId());
        preferencesService.saveWeekPreference(dto);
    }
}
