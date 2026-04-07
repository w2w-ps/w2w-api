package com.w2w.api.preferences;

import com.w2w.api.config.TenantContext;
import com.w2w.api.preferences.dto.*;
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
    public ResponseEntity<DayPreferenceResponse> getDayPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        DayPreferenceResponse preference = preferencesService.getDayPreference(employeeId, date).orElse(null);
        return ResponseEntity.ok(preference);
    }

    @GetMapping("/day/range")
    public ResponseEntity<List<DayPreferenceResponse>> getDayPreferencesRange(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        List<DayPreferenceResponse> preferences = preferencesService.getDayPreferencesInRange(employeeId, startDate, endDate);
        return ResponseEntity.ok(preferences);
    }

    @GetMapping("/week")
    public ResponseEntity<WeekPreferenceResponse> getWeekPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        WeekPreferenceResponse preference = preferencesService.getWeekPreference(employeeId, startDate).orElse(null);
        return ResponseEntity.ok(preference);
    }

    @GetMapping("/resolved")
    public ResponseEntity<List<ResolvedPreferenceResponse>> getResolvedPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        List<ResolvedPreferenceResponse> preferences = preferencesService.getResolvedPreferences(employeeId, startDate, endDate);
        return ResponseEntity.ok(preferences);
    }

    @PostMapping("/day")
    public void saveDayPreference(@Valid @RequestBody DayPreferenceRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        preferencesService.saveDayPreference(request);
    }

    @PostMapping("/day/repeat")
    public void saveDayPreferenceWithRepeat(@Valid @RequestBody DayPreferenceRepeatRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        preferencesService.saveDayPreferenceWithRepeat(request);
    }

    @PostMapping("/week")
    public void saveWeekPreference(@Valid @RequestBody WeekPreferenceRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        preferencesService.saveWeekPreference(request);
    }

    @PostMapping("/day/list")
    public void saveDayPreferenceList(@Valid @RequestBody DayPreferenceListRequest request) {
        if (request.preferences() != null && !request.preferences().isEmpty()) {
            TenantContext.setCurrentTenant(request.preferences().get(0).companyId());
            preferencesService.saveDayPreferenceList(request);
        }
    }
}
