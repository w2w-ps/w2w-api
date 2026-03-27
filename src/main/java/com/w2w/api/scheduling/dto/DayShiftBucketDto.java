package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DayShiftBucketDto {
    private LocalDate date;
    private List<ShiftDto> shifts = new ArrayList<>();

    public DayShiftBucketDto() {}

    public DayShiftBucketDto(LocalDate date, List<ShiftDto> shifts) {
        this.date = date;
        this.shifts = shifts;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<ShiftDto> getShifts() {
        return shifts;
    }

    public void setShifts(List<ShiftDto> shifts) {
        this.shifts = shifts;
    }
}
