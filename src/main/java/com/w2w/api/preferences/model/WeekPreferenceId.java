package com.w2w.api.preferences.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class WeekPreferenceId implements Serializable {
    private Integer employeeId;
    private LocalDate startDate;

    public WeekPreferenceId() {
        // Required by JPA for entity instantiation.
    }

    public WeekPreferenceId(Integer employeeId, LocalDate startDate) {
        this.employeeId = employeeId;
        this.startDate = startDate;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeekPreferenceId that = (WeekPreferenceId) o;
        return Objects.equals(employeeId, that.employeeId) && Objects.equals(startDate, that.startDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, startDate);
    }
}
