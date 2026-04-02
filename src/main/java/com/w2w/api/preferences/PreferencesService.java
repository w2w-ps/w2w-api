package com.w2w.api.preferences;

import com.w2w.api.config.TenantContext;
import com.w2w.api.preferences.dto.DayPreferenceDto;
import com.w2w.api.preferences.dto.WeekPreferenceDto;
import com.w2w.api.preferences.model.DayPreference;
import com.w2w.api.preferences.model.DayPreferenceId;
import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.model.WeekPreferenceId;
import com.w2w.api.preferences.repository.DayPreferenceRepository;
import com.w2w.api.preferences.repository.WeekPreferenceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PreferencesService {
    private final DayPreferenceRepository dayPreferenceRepository;
    private final WeekPreferenceRepository weekPreferenceRepository;

    public PreferencesService(DayPreferenceRepository dayPreferenceRepository,
                              WeekPreferenceRepository weekPreferenceRepository) {
        this.dayPreferenceRepository = dayPreferenceRepository;
        this.weekPreferenceRepository = weekPreferenceRepository;
    }

    public Optional<DayPreferenceDto> getDayPreference(Integer employeeId, LocalDate date) {
        return dayPreferenceRepository.findById(new DayPreferenceId(employeeId, date))
                .map(this::mapToDayDto);
    }

    public Optional<WeekPreferenceDto> getWeekPreference(Integer employeeId, LocalDate startDate) {
        return weekPreferenceRepository.findById(new WeekPreferenceId(employeeId, startDate))
                .map(this::mapToWeekDto);
    }

    public void saveDayPreference(DayPreferenceDto dto) {
        DayPreference entity = mapToDayEntity(dto);
        dayPreferenceRepository.save(entity);
    }

    public void saveWeekPreference(WeekPreferenceDto dto) {
        WeekPreference entity = mapToWeekEntity(dto);
        weekPreferenceRepository.save(entity);
    }

    private DayPreferenceDto mapToDayDto(DayPreference entity) {
        return new DayPreferenceDto(
                TenantContext.getCurrentTenant(),
                entity.getEmployeeId(),
                entity.getDate(),
                entity.getPrefs(),
                entity.getCompression(),
                entity.getEditedBy()
        );
    }

    private WeekPreferenceDto mapToWeekDto(WeekPreference entity) {
        return new WeekPreferenceDto(
                TenantContext.getCurrentTenant(),
                entity.getEmployeeId(),
                entity.getStartDate(),
                entity.getPrefs(),
                entity.getCompression(),
                entity.getEditedBy()
        );
    }

    private DayPreference mapToDayEntity(DayPreferenceDto dto) {
        DayPreference entity = new DayPreference();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setDate(dto.getDate());
        entity.setPrefs(dto.getPrefs());
        entity.setCompression(dto.getCompression());
        entity.setEditedBy(dto.getEditedBy());
        return entity;
    }

    private WeekPreference mapToWeekEntity(WeekPreferenceDto dto) {
        WeekPreference entity = new WeekPreference();
        entity.setEmployeeId(dto.getEmployeeId());
        entity.setStartDate(dto.getStartDate());
        entity.setPrefs(dto.getPrefs());
        entity.setCompression(dto.getCompression());
        entity.setEditedBy(dto.getEditedBy());
        return entity;
    }
}
