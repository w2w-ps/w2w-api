package com.w2w.api.preferences;

import com.w2w.api.preferences.dto.*;
import com.w2w.api.preferences.model.DayPreference;
import com.w2w.api.preferences.model.DayPreferenceId;
import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.repository.DayPreferenceRepository;
import com.w2w.api.preferences.repository.WeekPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PreferencesServiceTest {

    @Mock
    private DayPreferenceRepository dayPreferenceRepository;

    @Mock
    private WeekPreferenceRepository weekPreferenceRepository;

    @InjectMocks
    private PreferencesService preferencesService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getDayPreference_returnsPreference() {
        Integer employeeId = 1;
        LocalDate date = LocalDate.of(2026, 4, 1);
        DayPreference entity = new DayPreference();
        entity.setEmployeeId(employeeId);
        entity.setDate(date);
        entity.setPrefs("G".repeat(96));
        entity.setIsDayPrefs(true);

        when(dayPreferenceRepository.findById(any(DayPreferenceId.class))).thenReturn(Optional.of(entity));

        Optional<DayPreferenceResponse> result = preferencesService.getDayPreference(employeeId, date);

        assertTrue(result.isPresent());
        assertEquals("G".repeat(96), result.get().prefs());
        assertTrue(result.get().isDayPrefs());
    }

    @Test
    void saveDayPreference_validatesAndSaves() {
        DayPreferenceRequest request = new DayPreferenceRequest(
                1, 10, LocalDate.of(2026, 4, 1), "G", 0, 1, true);

        preferencesService.saveDayPreference(request);

        verify(dayPreferenceRepository).save(argThat(entity -> entity.getPrefs().equals("G".repeat(96)) &&
                entity.getIsDayPrefs()));
    }

    @Test
    void saveDayPreference_invalidPrefs_throwsException() {
        DayPreferenceRequest request = new DayPreferenceRequest(
                1, 10, LocalDate.of(2026, 4, 1), "AB", 0, 1, true);

        assertThrows(IllegalArgumentException.class, () -> preferencesService.saveDayPreference(request));
    }

    @Test
    void saveDayPreferenceWithRepeat_savesCorrectNumberOfTimes() {
        DayPreferenceRepeatRequest request = new DayPreferenceRepeatRequest(
                1, 10, LocalDate.of(2026, 4, 1), "G", 0, 1, 3, true);

        preferencesService.saveDayPreferenceWithRepeat(request);

        verify(dayPreferenceRepository, times(3)).save(any(DayPreference.class));
    }

    @Test
    void getResolvedPreferences_resolvesDayOverWeek() {
        Integer employeeId = 1;
        LocalDate startDate = LocalDate.of(2026, 4, 1); // Wednesday
        LocalDate endDate = LocalDate.of(2026, 4, 1);

        DayPreference dayPref = new DayPreference();
        dayPref.setDate(startDate);
        dayPref.setPrefs("D".repeat(96));
        dayPref.setIsDayPrefs(true);

        WeekPreference weekPref = new WeekPreference();
        weekPref.setPrefs("W".repeat(672));
        weekPref.setStartDate(LocalDate.of(2026, 3, 30)); // Monday

        when(dayPreferenceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate))
                .thenReturn(List.of(dayPref));
        when(weekPreferenceRepository.findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(employeeId,
                startDate))
                .thenReturn(Optional.of(weekPref));

        List<ResolvedPreferenceResponse> result = preferencesService.getResolvedPreferences(employeeId, startDate,
                endDate);

        assertEquals(1, result.size());
        assertEquals("D".repeat(96), result.get(0).prefs());
        assertEquals("DAY", result.get(0).preferenceType());
    }

    @Test
    void getResolvedPreferences_resolvesWeekWhenDayMissing() {
        Integer employeeId = 1;
        LocalDate startDate = LocalDate.of(2026, 4, 1); // Wednesday
        LocalDate endDate = LocalDate.of(2026, 4, 1);

        WeekPreference weekPref = new WeekPreference();
        // 672 chars total. Wednesday is index 2. 2*96 = 192.
        String weekPrefs = "A".repeat(96) + "B".repeat(96) + "C".repeat(96) + "D".repeat(96) + "E".repeat(96)
                + "F".repeat(96) + "G".repeat(96);
        weekPref.setPrefs(weekPrefs);
        weekPref.setStartDate(LocalDate.of(2026, 3, 30)); // Monday

        when(dayPreferenceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate))
                .thenReturn(List.of());
        when(weekPreferenceRepository.findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(employeeId,
                startDate))
                .thenReturn(Optional.of(weekPref));

        List<ResolvedPreferenceResponse> result = preferencesService.getResolvedPreferences(employeeId, startDate,
                endDate);

        assertEquals(1, result.size());
        assertEquals("C".repeat(96), result.get(0).prefs());
        assertEquals("WEEK", result.get(0).preferenceType());
    }
}
