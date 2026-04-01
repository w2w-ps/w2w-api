package com.w2w.api.employee;

import jakarta.persistence.*;
import com.w2w.api.login.User;

import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "status")
    private String status;

    @Column(name = "last_logon")
    private LocalDateTime lastLogon;

    @Column(name = "logon_count")
    private Integer logonCount;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "email")
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "employee_phone",
            joinColumns = @JoinColumn(name = "employee_id")
    )
    @Column(name = "phone_number")
    @OrderColumn(name = "sort_order")
    private List<String> phones = new ArrayList<>();

    @Column(name = "hire_date")
    private LocalDateTime hireDate;

    @Column(name = "max_scheduled_hours")
    private Integer maxScheduledHours;

    @Column(name = "max_daily_hours")
    private Integer maxDailyHours;

    @Column(name = "pay_rate")
    private Float payRate;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private User user;

    public Employee() {}

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLogon() {
        return lastLogon;
    }

    public void setLastLogon(LocalDateTime lastLogon) {
        this.lastLogon = lastLogon;
    }

    public Integer getLogonCount() {
        return logonCount;
    }

    public void setLogonCount(Integer logonCount) {
        this.logonCount = logonCount;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmployeeNumber() {
        return employeeNumber;
    }

    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public LocalDateTime getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDateTime hireDate) {
        this.hireDate = hireDate;
    }

    public Integer getMaxScheduledHours() {
        return maxScheduledHours;
    }

    public void setMaxScheduledHours(Integer maxScheduledHours) {
        this.maxScheduledHours = maxScheduledHours;
    }

    public Integer getMaxDailyHours() {
        return maxDailyHours;
    }

    public void setMaxDailyHours(Integer maxDailyHours) {
        this.maxDailyHours = maxDailyHours;
    }

    public Float getPayRate() {
        return payRate;
    }

    public void setPayRate(Float payRate) {
        this.payRate = payRate;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
