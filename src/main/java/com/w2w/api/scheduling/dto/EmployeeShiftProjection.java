package com.w2w.api.scheduling.dto;

import com.w2w.api.position.dto.PositionSummary;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface EmployeeShiftProjection {
    Integer getShiftId();
    Integer getEmployeeId();
    String getFirstName();
    String getLastName();
    default String getEmploymentType() {
        return null;
    }
    default Integer getEmpTypeId() {
        return null;
    }
    default LocalDate getAlertDate() {
        return null;
    }
    List<String> getPhones();
    List<PositionSummary> getAvailablePositions();
    LocalDate getWeekCommencing();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Integer getPositionId();
    String getPosition();
    Integer getCategoryId();
    String getCategory();
    String getCategoryShortDescription();
    String getDescription();
    Float getDuration();
    Boolean getIsOvernight();
    Boolean getSchedulePublished();
    String getColor();
}
