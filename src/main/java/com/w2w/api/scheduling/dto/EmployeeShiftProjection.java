package com.w2w.api.scheduling.dto;

import com.w2w.api.position.dto.PositionDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EmployeeShiftProjection {
    Integer getEmployeeId();
    String getFirstName();
    String getLastName();
    List<String> getPhones();
    List<PositionDto> getAvailablePositions();
    LocalDate getWeekCommencing();
    LocalTime getStartTime();
    LocalTime getEndTime();
    String getPosition();
    String getCategory();
    String getDescription();
    Float getDuration();
    Boolean getIsOvernight();
}
