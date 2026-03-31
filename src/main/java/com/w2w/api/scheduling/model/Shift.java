package com.w2w.api.scheduling.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "scheduled_employee")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "schedule_id")
    private Integer scheduleId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "description")
    private String description;

    @Column(name = "start_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
    private LocalTime startTime;

    @Column(name = "end_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma")
    private LocalTime endTime;

    @Column(name = "duration")
    private Float duration;

    @Column(name = "is_overnight")
    private Boolean isOvernight;

    @Column(name = "required_skill_id")
    private Integer requiredSkillId;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "color")
    private Short color;

    @Column(name = "changed_by")
    private Integer changedBy;

    public Shift() {}

    public Integer getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Integer transactionId) {
        this.transactionId = transactionId;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Float getDuration() {
        return duration;
    }

    public void setDuration(Float duration) {
        this.duration = duration;
    }

    public Boolean getIsOvernight() {
        return isOvernight;
    }

    public void setIsOvernight(Boolean isOvernight) {
        this.isOvernight = isOvernight;
    }

    public Integer getRequiredSkillId() {
        return requiredSkillId;
    }

    public void setRequiredSkillId(Integer requiredSkillId) {
        this.requiredSkillId = requiredSkillId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Short getColor() {
        return color;
    }

    public void setColor(Short color) {
        this.color = color;
    }

    public Integer getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Integer changedBy) {
        this.changedBy = changedBy;
    }
}
