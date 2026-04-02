package com.w2w.api.preferences.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class WeekPreferenceDto {
    @NotNull
    @Positive
    private Integer employeeId;

    @NotNull
    @Positive
    private Integer companyId;

    @NotNull
    private LocalDate startDate;

    @Size(max = 700)
    private String prefs;

    private Integer compression;

    @Positive
    private Integer editedBy;

    public WeekPreferenceDto() {
    }

    public WeekPreferenceDto(Integer companyId, Integer employeeId, LocalDate startDate, String prefs,
            Integer compression, Integer editedBy) {
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.startDate = startDate;
        this.prefs = prefs;
        this.compression = compression;
        this.editedBy = editedBy;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
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
