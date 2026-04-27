package com.w2w.api.preferences;

import com.w2w.api.preferences.dto.*;
import com.w2w.api.preferences.model.DayPreference;
import com.w2w.api.preferences.model.DayPreferenceId;
import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.model.WeekPreferenceId;
import com.w2w.api.preferences.repository.DayPreferenceRepository;
import com.w2w.api.preferences.repository.WeekPreferenceRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
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
                .toList();
    }

    public Optional<WeekPreferenceResponse> getWeekPreference(Integer employeeId, LocalDate date) {
        return weekPreferenceRepository.findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(employeeId, date)
                .map(this::mapToWeekResponse);
    }

    public Optional<String> getResolvedPreference(Integer employeeId, LocalDate date) {
        if (employeeId == null || date == null) {
            return Optional.empty();
        }

        Optional<DayPreference> dayPreference = dayPreferenceRepository.findById(new DayPreferenceId(employeeId, date));
        if (dayPreference.isPresent()) {
            return Optional.ofNullable(dayPreference.get().getPrefs());
        }

        WeekPreference weekPreference = weekPreferenceRepository
                .findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(employeeId, date)
                .orElse(null);
        return Optional.ofNullable(resolveWeekPreferenceForDate(weekPreference, date));
    }

    public void saveDayPreference(DayPreferenceRequest request) {
        DayPreference entity = mapToDayEntity(request);
        dayPreferenceRepository.save(entity);
    }

    public void saveDayPreferenceWithRepeat(DayPreferenceRepeatRequest request) {
        validateDayPrefs(request.isDayPrefs(), request.prefs());
        String effectivePrefs = getEffectivePrefs(request.isDayPrefs(), request.prefs());
        LocalDate currentDate = request.date();
        for (int i = 0; i < request.repeatCount(); i++) {
            DayPreference entity = new DayPreference();
            entity.setEmployeeId(request.employeeId());
            entity.setDate(currentDate);
            entity.setPrefs(effectivePrefs);
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
                .toList();
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
        entity.setPrefs(getEffectivePrefs(request.isDayPrefs(), request.prefs()));
        entity.setCompression(request.compression());
        entity.setEditedBy(request.editedBy());
        entity.setIsDayPrefs(request.isDayPrefs());
        return entity;
    }

    private void validateDayPrefs(Boolean isDayPrefs, String prefs) {
        if (prefs == null) {
            throw new IllegalArgumentException("Preference string cannot be null.");
        }

        if (Boolean.TRUE.equals(isDayPrefs)) {
            validateFullDayPrefs(prefs);
            return;
        }

        if (prefs.length() != 96) {
            throw new IllegalArgumentException("When isDayPrefs is false or null, exactly 96 characters are required.");
        }
    }

    private void validateFullDayPrefs(String prefs) {
        if (prefs.length() != 1 && prefs.length() != 96) {
            throw new IllegalArgumentException("When isDayPrefs is true, preference must be either 1 or 96 characters.");
        }

        if (prefs.length() == 96 && !hasSameCharacterForEverySlot(prefs)) {
            throw new IllegalArgumentException("When isDayPrefs is true, all 96 characters must be the same.");
        }
    }

    private boolean hasSameCharacterForEverySlot(String prefs) {
        char firstChar = prefs.charAt(0);
        for (int i = 1; i < prefs.length(); i++) {
            if (prefs.charAt(i) != firstChar) {
                return false;
            }
        }
        return true;
    }

    private String getEffectivePrefs(Boolean isDayPrefs, String prefs) {
        if (Boolean.TRUE.equals(isDayPrefs) && prefs != null && prefs.length() == 1) {
            return prefs.repeat(96);
        }
        return prefs;
    }

    private WeekPreference mapToWeekEntity(WeekPreferenceRequest request) {
        WeekPreference entity = new WeekPreference();
        entity.setEmployeeId(request.employeeId());
        
        LocalDate monday = request.startDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        entity.setStartDate(monday);
        
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

        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            WeekPreference weekPref = weekPreferenceRepository
                    .findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(employeeId, currentDate)
                    .orElse(null);

            DayPreference dayPref = dayPrefsMap.get(currentDate);
            String prefs = resolvePreference(currentDate, dayPref, weekPref);
            String type = "NONE";

            if (dayPref != null) {
                type = Boolean.TRUE.equals(dayPref.getIsDayPrefs()) ? "DAY" : "HOUR";
            } else if (prefs != null) {
                type = "WEEK";
            }

            String day = currentDate.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH);
            result.add(new ResolvedPreferenceResponse(currentDate, prefs, type, day));
            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    private String resolvePreference(LocalDate date, DayPreference dayPreference, WeekPreference weekPreference) {
        if (dayPreference != null) {
            return dayPreference.getPrefs();
        }
        return resolveWeekPreferenceForDate(weekPreference, date);
    }

    private String resolveWeekPreferenceForDate(WeekPreference weekPreference, LocalDate date) {
        if (weekPreference == null || weekPreference.getPrefs() == null || weekPreference.getPrefs().length() < 672) {
            return null;
        }

        int dayIndex = date.getDayOfWeek().getValue() - 1;
        int startIndex = dayIndex * 96;
        return weekPreference.getPrefs().substring(startIndex, Math.min(startIndex + 96, weekPreference.getPrefs().length()));
    }
}
