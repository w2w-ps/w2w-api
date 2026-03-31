package com.w2w.api.scheduling;

import com.w2w.api.scheduling.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
    Optional<Schedule> findByCompanyIdAndStartDate(Integer companyId, LocalDate startDate);
}
