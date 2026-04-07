package com.w2w.api.preferences;

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

    public List<DayPreferenceResponse> getDayPreferencesInRange(Integer employeeId, LocalDate startDate,
            LocalDate endDate) {
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
        validateDayPrefs(request.isDayPrefs(), request.prefs());
        LocalDate currentDate = request.date();
        for (int i = 0; i < request.repeatCount(); i++) {
            DayPreference entity = new DayPreference();
            entity.setEmployeeId(request.employeeId());
            entity.setDate(currentDate);
            entity.setPrefs(request.prefs());
            entity.setCompression(request.compression());
            entity.setEditedBy(request.editedBy());
            entity.setIsDayPrefs(request.isDayPrefs());

            dayPreferenceRepository.save(entity);
            currentDate = currentDate.plusWeeks(1);
        }
    }

    public void saveWeekPreference(WeekPreferenceRequest request) {
        WeekPreference entity = mapToWeekEntity(request);
        weekPreferenceRepository.save(entity);
    }

    public void saveDayPreferenceList(DayPreferenceListRequest request) {
        List<DayPreference> entities = request.preferences().stream()
                .map(this::mapToDayEntity)
                .collect(Collectors.toList());
        dayPreferenceRepository.saveAll(entities);
    }

    private DayPreferenceResponse mapToDayResponse(DayPreference entity) {
        return new DayPreferenceResponse(
                entity.getDate(),
                entity.getPrefs(),
                entity.getIsDayPrefs());
    }

    private WeekPreferenceResponse mapToWeekResponse(WeekPreference entity) {
        return new WeekPreferenceResponse(
                entity.getStartDate(),
                entity.getPrefs());
    }

    private DayPreference mapToDayEntity(DayPreferenceRequest request) {
        validateDayPrefs(request.isDayPrefs(), request.prefs());
        DayPreference entity = new DayPreference();
        entity.setEmployeeId(request.employeeId());
        entity.setDate(request.date());
        entity.setPrefs(request.prefs());
        entity.setCompression(request.compression());
        entity.setEditedBy(request.editedBy());
        entity.setIsDayPrefs(request.isDayPrefs());
        return entity;
    }

    private void validateDayPrefs(Boolean isDayPrefs, String prefs) {
        if (Boolean.TRUE.equals(isDayPrefs) && prefs != null && prefs.length() > 1) {
            char firstChar = prefs.charAt(0);
            for (int i = 1; i < prefs.length(); i++) {
                if (prefs.charAt(i) != firstChar) {
                    throw new IllegalArgumentException("When isDayPrefs is true, all the prefs must be the same.");
                }
            }
        }
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

    public List<ResolvedPreferenceResponse> getResolvedPreferences(Integer employeeId, LocalDate startDate,
            LocalDate endDate) {
        List<ResolvedPreferenceResponse> result = new java.util.ArrayList<>();

        java.util.Map<LocalDate, DayPreference> dayPrefsMap = dayPreferenceRepository
                .findByEmployeeIdAndDateBetween(employeeId, startDate, endDate)
                .stream()
                .collect(Collectors.toMap(DayPreference::getDate, p -> p));

        java.util.Map<LocalDate, WeekPreference> weekPrefsMap = new java.util.HashMap<>();

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            WeekPreference weekPref = weekPreferenceRepository
                    .findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(employeeId, currentDate)
                    .orElse(null);

            DayPreference dayPref = dayPrefsMap.get(currentDate);
            String prefs = null;
            String type = "NONE";

            if (dayPref != null) {
                prefs = dayPref.getPrefs();
                type = Boolean.TRUE.equals(dayPref.getIsDayPrefs()) ? "DAY" : "HOUR";
            } else if (weekPref != null && weekPref.getPrefs() != null && weekPref.getPrefs().length() >= 672) {
                int dayIndex = currentDate.getDayOfWeek().getValue() - 1; // 0 for Monday, 6 for Sunday
                int startIdx = dayIndex * 96;
                prefs = weekPref.getPrefs().substring(startIdx, Math.min(startIdx + 96, weekPref.getPrefs().length()));
                type = "WEEK";
            }

            result.add(new ResolvedPreferenceResponse(currentDate, prefs, type));
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }
}
