package com.w2w.api.scheduling.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "schedule_partial_pub")
@IdClass(SchedulePartialPubId.class)
public class SchedulePartialPub {
    @Id
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Id
    @Column(name = "required_position_id")
    private Integer requiredPositionId;

    @Column(name = "published_by_id")
    private Integer publishedById;

    public SchedulePartialPub() {}

    public SchedulePartialPub(Integer scheduleId, Integer requiredPositionId, Integer publishedById) {
        this.scheduleId = scheduleId;
        this.requiredPositionId = requiredPositionId;
        this.publishedById = publishedById;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getRequiredPositionId() {
        return requiredPositionId;
    }

    public void setRequiredPositionId(Integer requiredPositionId) {
        this.requiredPositionId = requiredPositionId;
    }

    public Integer getPublishedById() {
        return publishedById;
    }

    public void setPublishedById(Integer publishedById) {
        this.publishedById = publishedById;
    }
}

class SchedulePartialPubId implements Serializable {
    private Integer scheduleId;
    private Integer requiredPositionId;

    public SchedulePartialPubId() {}

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
