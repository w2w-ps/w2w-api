package com.w2w.api.preferences;

import com.w2w.api.config.TenantContext;
import com.w2w.api.preferences.dto.*;
import com.w2w.api.preferences.model.DayPreference;
import com.w2w.api.preferences.model.DayPreferenceId;
import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.model.WeekPreferenceId;
import com.w2w.api.preferences.repository.DayPreferenceRepository;
import com.w2w.api.preferences.repository.WeekPreferenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PreferencesService {
    private final DayPreferenceRepository dayPreferenceRepository;
    private final WeekPreferenceRepository weekPreferenceRepository;

    public PreferencesService(DayPreferenceRepository dayPreferenceRepository,
                              WeekPreferenceRepository weekPreferenceRepository) {
        this.dayPreferenceRepository = dayPreferenceRepository;
        this.weekPreferenceRepository = weekPreferenceRepository;
    }

    public Optional<DayPreferenceResponse> getDayPreference(Integer employeeId, LocalDate date) {
        return dayPreferenceRepository.findById(new DayPreferenceId(employeeId, date))
                .map(this::mapToDayResponse);
    }

    public List<DayPreferenceResponse> getDayPreferencesInRange(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        return dayPreferenceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate)
                .stream()
                .map(this::mapToDayResponse)
                .collect(Collectors.toList());
    }

    public Optional<WeekPreferenceResponse> getWeekPreference(Integer employeeId, LocalDate startDate) {
        return weekPreferenceRepository.findById(new WeekPreferenceId(employeeId, startDate))
                .map(this::mapToWeekResponse);
    }

    public void saveDayPreference(DayPreferenceRequest request) {
        DayPreference entity = mapToDayEntity(request);
        dayPreferenceRepository.save(entity);
    }

    public void saveDayPreferenceWithRepeat(DayPreferenceRepeatRequest request) {
        LocalDate currentDate = request.date();
        for (int i = 0; i < request.repeatCount(); i++) {
            DayPreference entity = new DayPreference();
            entity.setEmployeeId(request.employeeId());
            entity.setDate(currentDate);
            entity.setPrefs(request.prefs());
            entity.setCompression(request.compression());
            entity.setEditedBy(request.editedBy());
            
            dayPreferenceRepository.save(entity);
            currentDate = currentDate.plusWeeks(1);
        }
    }

    public void saveWeekPreference(WeekPreferenceRequest request) {
        WeekPreference entity = mapToWeekEntity(request);
        weekPreferenceRepository.save(entity);
    }

    private DayPreferenceResponse mapToDayResponse(DayPreference entity) {
        return new DayPreferenceResponse(
                entity.getEmployeeId(),
                TenantContext.getCurrentTenant(),
                entity.getDate(),
                entity.getPrefs(),
                entity.getCompression(),
                entity.getEditedBy()
        );
    }

    private WeekPreferenceResponse mapToWeekResponse(WeekPreference entity) {
        return new WeekPreferenceResponse(
                entity.getEmployeeId(),
                TenantContext.getCurrentTenant(),
                entity.getStartDate(),
                entity.getPrefs(),
                entity.getCompression(),
                entity.getEditedBy()
        );
    }

    private DayPreference mapToDayEntity(DayPreferenceRequest request) {
        DayPreference entity = new DayPreference();
        entity.setEmployeeId(request.employeeId());
        entity.setDate(request.date());
        entity.setPrefs(request.prefs());
        entity.setCompression(request.compression());
        entity.setEditedBy(request.editedBy());
        return entity;
    }

    private WeekPreference mapToWeekEntity(WeekPreferenceRequest request) {
        WeekPreference entity = new WeekPreference();
        entity.setEmployeeId(request.employeeId());
        entity.setStartDate(request.startDate());
        entity.setPrefs(request.prefs());
        entity.setCompression(request.compression());
        entity.setEditedBy(request.editedBy());
        return entity;
    }
}
