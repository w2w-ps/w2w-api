package com.w2w.api.preferences.repository;

import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.model.WeekPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeekPreferenceRepository extends JpaRepository<WeekPreference, WeekPreferenceId> {
    Optional<WeekPreference> findFirstByEmployeeIdAndStartDateBetweenOrderByStartDateDesc(Integer employeeId, LocalDate start, LocalDate end);
    Optional<WeekPreference> findFirstByEmployeeIdAndStartDateLessThanEqualOrderByStartDateDesc(Integer employeeId, LocalDate date);
}
