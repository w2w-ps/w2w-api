package com.w2w.api.preferences.repository;

import com.w2w.api.preferences.model.DayPreference;
import com.w2w.api.preferences.model.DayPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DayPreferenceRepository extends JpaRepository<DayPreference, DayPreferenceId> {
}
