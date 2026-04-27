package com.w2w.api.preferences.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "week_prefs")
@IdClass(WeekPreferenceId.class)
public class WeekPreference {
    @Id
    @Column(name = "employee_id")
    private Integer employeeId;

    @Id
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "prefs", length = 672)
    private String prefs;

    @Column(name = "compression")
    private Integer compression;

    @Column(name = "edited_by")
    private Integer editedBy;

    public WeekPreference() {
        // Required by JPA for entity instantiation.
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
}
