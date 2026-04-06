package com.w2w.api.preferences.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class DayPreferenceDto {
    @NotNull
    @Positive
    private Integer employeeId;
    
    @NotNull
    @Positive
    private Integer companyId;
    
    @NotNull
    private LocalDate date;
    
    @Size(max = 96)
    private String prefs;
    
    private Integer compression;
    
    @Positive
    private Integer editedBy;
    
    private String day;

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
        if (date != null) {
            this.day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
    }

    public DayPreferenceDto(Integer companyId, Integer employeeId, LocalDate date, String day, String prefs, Integer compression,
            Integer editedBy) {
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.date = date;
        this.day = day;
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
        if (date != null) {
            this.day = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
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

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }
}
