package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalTime;

public class ShiftDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
    private LocalTime startTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
    private LocalTime endTime;
    private String position;
    private String category;
    private String description;
    private Float duration;

    public ShiftDto() {}

    public ShiftDto(LocalTime startTime, LocalTime endTime, String position, String category, String description, Float duration) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.position = position;
        this.category = category;
        this.description = description;
        this.duration = duration;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Float getDuration() {
        return duration;
    }

    public void setDuration(Float duration) {
        this.duration = duration;
    }
}
