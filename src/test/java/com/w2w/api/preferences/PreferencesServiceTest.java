package com.w2w.api.preferences;

import com.w2w.api.preferences.model.DayPreference;
import com.w2w.api.preferences.model.DayPreferenceId;
import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.repository.DayPreferenceRepository;
import com.w2w.api.preferences.repository.WeekPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PreferencesServiceTest {
    private DayPreferenceRepository dayPreferenceRepository;
    private WeekPreferenceRepository weekPreferenceRepository;
    private PreferencesService preferencesService;

    @BeforeEach
    void setUp() {
        dayPreferenceRepository = mock(DayPreferenceRepository.class);
        weekPreferenceRepository = mock(WeekPreferenceRepository.class);
        preferencesService = new PreferencesService(dayPreferenceRepository, weekPreferenceRepository);
    }

    @Test
    void getResolvedPreference_returnsDayPreferenceWhenPresent() {
        LocalDate date = LocalDate.of(2026, 4, 21);
        DayPreference dayPreference = new DayPreference();
        dayPreference.setEmployeeId(101);
        dayPreference.setDate(date);
        dayPreference.setPrefs("D".repeat(96));

        when(dayPreferenceRepository.findById(new DayPreferenceId(101, date)))
                .thenReturn(Optional.of(dayPreference));

        Optional<String> resolvedPreference = preferencesService.getResolvedPreference(101, date);

        assertEquals(Optional.of("D".repeat(96)), resolvedPreference);
        verify(dayPreferenceRepository).findById(new DayPreferenceId(101, date));
        verifyNoInteractions(weekPreferenceRepository);
    }

    @Test
    void getResolvedPreference_returnsWeekSliceWhenDayPreferenceMissing() {
        LocalDate date = LocalDate.of(2026, 4, 22);
        WeekPreference weekPreference = new WeekPreference();
        weekPreference.setEmployeeId(101);
        weekPreference.setStartDate(LocalDate.of(2026, 4, 20));
        weekPreference.setPrefs(
                "P".repeat(96)
                        + "C".repeat(96)
                        + "D".repeat(96)
                        + "N".repeat(96)
                        + "P".repeat(96)
                        + "C".repeat(96)
                        + "D".repeat(96)
        );

        when(dayPreferenceRepository.findById(new DayPreferenceId(101, date)))
                .thenReturn(Optional.empty());
        when(weekPreferenceRepository.findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(101, date))
                .thenReturn(Optional.of(weekPreference));

        Optional<String> resolvedPreference = preferencesService.getResolvedPreference(101, date);

        assertEquals(Optional.of("D".repeat(96)), resolvedPreference);
    }

    @Test
    void getResolvedPreference_returnsEmptyWhenNoPreferencesExist() {
        LocalDate date = LocalDate.of(2026, 4, 21);

        when(dayPreferenceRepository.findById(new DayPreferenceId(101, date)))
                .thenReturn(Optional.empty());
        when(weekPreferenceRepository.findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(101, date))
                .thenReturn(Optional.empty());

        Optional<String> resolvedPreference = preferencesService.getResolvedPreference(101, date);

        assertTrue(resolvedPreference.isEmpty());
    }
}
