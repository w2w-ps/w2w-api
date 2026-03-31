package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

public class ShiftResponseDto {
    private Integer shiftId;
    private Integer employeeId;
    private Integer companyId;
    private String description;
    private LocalDate date;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
    private LocalTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
    private LocalTime endTime;
    
    private Float duration;
    private Boolean isOvernight;
    private String position;
    private String category;
    private String color;

    public ShiftResponseDto() {}

    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public Integer getCompanyId() { return companyId; }
    public void setCompanyId(Integer companyId) { this.companyId = companyId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public Float getDuration() { return duration; }
    public void setDuration(Float duration) { this.duration = duration; }

    public Boolean getIsOvernight() { return isOvernight; }
    public void setIsOvernight(Boolean isOvernight) { this.isOvernight = isOvernight; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
