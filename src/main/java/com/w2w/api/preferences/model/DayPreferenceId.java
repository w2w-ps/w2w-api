package com.w2w.api.preferences.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DayPreferenceId implements Serializable {
    private Integer employeeId;
    private LocalDate date;

    public DayPreferenceId() {
        // Required by JPA for entity instantiation.
    }

    public DayPreferenceId(Integer employeeId, LocalDate date) {
        this.employeeId = employeeId;
        this.date = date;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DayPreferenceId that = (DayPreferenceId) o;
        return Objects.equals(employeeId, that.employeeId) && Objects.equals(date, that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employeeId, date);
    }
}
