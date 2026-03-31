package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public interface ShiftDetailsProjection {
    Integer getTransactionId();

    Integer getEmployeeId();

    Integer getCompanyId();

    String getDescription();

    LocalDate getDate();

    LocalTime getStartTime();

    LocalTime getEndTime();

    Float getDuration();

    Boolean getIsOvernight();

    String getPosition();

    String getCategory();

    Short getColor();
}
