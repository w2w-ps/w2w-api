package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public interface DailyHoursShiftProjection {
    Integer getShiftId();
    LocalDate getDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Float getDuration();
}
