package com.w2w.api.preferences.dto;

import java.time.LocalDate;

public class DayPreferenceDto {
    private Integer employeeId;
    private Integer companyId;
    private LocalDate date;
    private String prefs;
    private Integer compression;
    private Integer editedBy;

    public DayPreferenceDto() {
    }

    public DayPreferenceDto(Integer companyId, Integer employeeId, LocalDate date, String prefs, Integer compression,
            Integer editedBy) {
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.date = date;
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
}
