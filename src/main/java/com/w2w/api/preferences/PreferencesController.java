package com.w2w.api.preferences;

import com.w2w.api.config.TenantContext;
import com.w2w.api.preferences.dto.DayPreferenceDto;
import com.w2w.api.preferences.dto.WeekPreferenceDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    @Autowired
    private PreferencesService preferencesService;

    @GetMapping("/day")
    public DayPreferenceDto getDayPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        return preferencesService.getDayPreference(employeeId, date).orElse(null);
    }

    @GetMapping("/week")
    public WeekPreferenceDto getWeekPreferences(
            @RequestParam Integer employeeId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        return preferencesService.getWeekPreference(employeeId, startDate).orElse(null);
    }

    @PostMapping("/day")
    public void saveDayPreference(@RequestBody DayPreferenceDto dto) {
        TenantContext.setCurrentTenant(dto.getCompanyId());
        preferencesService.saveDayPreference(dto);
    }

    @PostMapping("/week")
    public void saveWeekPreference(@RequestBody WeekPreferenceDto dto) {
        TenantContext.setCurrentTenant(dto.getCompanyId());
        preferencesService.saveWeekPreference(dto);
    }
}
