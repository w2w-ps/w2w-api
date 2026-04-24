package com.w2w.api.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "company")
public class Company {
    @Id
    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "timezone")
    private Float timezone;

    @Column(name = "status")
    private String status;

    @Column(name = "trial_start")
    private LocalDate trialStart;

    @Column(name = "drop_dead")
    private LocalDate dropDead;

    @Column(name = "price_table")
    private Integer priceTable;

    @Column(name = "tos")
    private String tos;

    @Min(1)
    @Max(7)
    @Column(name = "week_start_day")
    private Integer weekStartDay;

    public Company() {
    }

    public Integer getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Integer companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Float getTimezone() {
        return timezone;
    }

    public void setTimezone(Float timezone) {
        this.timezone = timezone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getTrialStart() {
        return trialStart;
    }

    public void setTrialStart(LocalDate trialStart) {
        this.trialStart = trialStart;
    }

    public LocalDate getDropDead() {
        return dropDead;
    }

    public void setDropDead(LocalDate dropDead) {
        this.dropDead = dropDead;
    }

    public Integer getPriceTable() {
        return priceTable;
    }

    public void setPriceTable(Integer priceTable) {
        this.priceTable = priceTable;
    }

    public String getTos() {
        return tos;
    }

    public void setTos(String tos) {
        this.tos = tos;
    }

    public Integer getWeekStartDay() {
        return weekStartDay;
    }

    public void setWeekStartDay(Integer weekStartDay) {
        this.weekStartDay = weekStartDay;
    }
}
