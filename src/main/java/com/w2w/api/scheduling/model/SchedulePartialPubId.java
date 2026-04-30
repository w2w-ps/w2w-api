package com.w2w.api.scheduling.model;

import java.io.Serializable;
import java.util.Objects;

public class SchedulePartialPubId implements Serializable {
    private Integer scheduleId;
    private Integer requiredPositionId;

    public SchedulePartialPubId() {
        // Required by JPA for entity instantiation.
    }

    public SchedulePartialPubId(Integer scheduleId, Integer requiredPositionId) {
        this.scheduleId = scheduleId;
        this.requiredPositionId = requiredPositionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchedulePartialPubId that = (SchedulePartialPubId) o;
        return Objects.equals(scheduleId, that.scheduleId) && Objects.equals(requiredPositionId, that.requiredPositionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheduleId, requiredPositionId);
    }
}
