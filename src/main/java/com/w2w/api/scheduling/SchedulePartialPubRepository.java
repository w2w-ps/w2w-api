package com.w2w.api.scheduling;

import com.w2w.api.scheduling.model.SchedulePartialPub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SchedulePartialPubRepository extends JpaRepository<SchedulePartialPub, Object> {
    List<SchedulePartialPub> findByScheduleId(Integer scheduleId);
    void deleteByScheduleId(Integer scheduleId);
}
