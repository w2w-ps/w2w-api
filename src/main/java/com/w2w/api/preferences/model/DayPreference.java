package com.w2w.api.preferences.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "day_prefs")
@IdClass(DayPreferenceId.class)
public class DayPreference {
    @Id
    @Column(name = "employee_id")
    private Integer employeeId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @Column(name = "prefs", length = 96)
    private String prefs;

    @Column(name = "compression")
    private Integer compression;

    @Column(name = "edited_by")
    private Integer editedBy;

    @Column(name = "is_day_prefs")
    private Boolean isDayPrefs;

    public DayPreference() {}

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

    public String getPrefs() {
        return prefs;
    }

    public void setPrefs(String prefs) {
        this.prefs = prefs;
    }

    public Integer getCompression() {
        return compression;
    }

    public void setCompression(Integer compression) {
        this.compression = compression;
    }

    public Integer getEditedBy() {
        return editedBy;
    }

    public void setEditedBy(Integer editedBy) {
        this.editedBy = editedBy;
    }

    public Boolean getIsDayPrefs() {
        return isDayPrefs;
    }

    public void setIsDayPrefs(Boolean isDayPrefs) {
        this.isDayPrefs = isDayPrefs;
    }
}
