package com.w2w.api.preferences.repository;

import com.w2w.api.preferences.model.WeekPreference;
import com.w2w.api.preferences.model.WeekPreferenceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeekPreferenceRepository extends JpaRepository<WeekPreference, WeekPreferenceId> {
}
