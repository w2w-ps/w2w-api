package com.w2w.api.employee.model;

import com.w2w.api.login.EmpType;
import com.w2w.api.position.model.Position;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "company_id")
    private Integer companyId;

    @Column(name = "status")
    private String status;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

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
    @CollectionTable(name = "employee_phone", joinColumns = @JoinColumn(name = "employee_id"))
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "emp_type_id")
    private EmpType empType;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private EmployeeAddress address;

    @Column(name = "max_weekly_days")
    private Integer maxWeeklyDays;

    @Column(name = "max_daily_shifts")
    private Integer maxDailyShifts;

    @Column(name = "comments")
    private String comments;

    @Column(name = "priority_group")
    private String priorityGroup;

    @Column(name = "google_cal_export")
    private Boolean googleCalExport = false;

    @Column(name = "next_alert_date")
    private LocalDate nextAlertDate;

    @Column(name = "custom_field_1")
    private String customField1;

    @Column(name = "custom_field_2")
    private String customField2;

    @Column(name = "employee_photo")
    private String employeePhoto;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "employee_position",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "position_id")
    )
    private List<Position> positions = new ArrayList<>();

    public Employee() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
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

    public EmpType getEmpType() {
        return empType;
    }

    public void setEmpType(EmpType empType) {
        this.empType = empType;
    }

    public EmployeeAddress getAddress() {
        return address;
    }

    public void setAddress(EmployeeAddress address) {
        this.address = address;
    }

    public Integer getMaxWeeklyDays() {
        return maxWeeklyDays;
    }

    public void setMaxWeeklyDays(Integer maxWeeklyDays) {
        this.maxWeeklyDays = maxWeeklyDays;
    }

    public Integer getMaxDailyShifts() {
        return maxDailyShifts;
    }

    public void setMaxDailyShifts(Integer maxDailyShifts) {
        this.maxDailyShifts = maxDailyShifts;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getPriorityGroup() {
        return priorityGroup;
    }

    public void setPriorityGroup(String priorityGroup) {
        this.priorityGroup = priorityGroup;
    }

    public Boolean getGoogleCalExport() {
        return googleCalExport;
    }

    public void setGoogleCalExport(Boolean googleCalExport) {
        this.googleCalExport = googleCalExport;
    }

    public LocalDate getNextAlertDate() {
        return nextAlertDate;
    }

    public void setNextAlertDate(LocalDate nextAlertDate) {
        this.nextAlertDate = nextAlertDate;
    }

    public String getCustomField1() {
        return customField1;
    }

    public void setCustomField1(String customField1) {
        this.customField1 = customField1;
    }

    public String getCustomField2() {
        return customField2;
    }

    public void setCustomField2(String customField2) {
        this.customField2 = customField2;
    }

    public String getEmployeePhoto() {
        return employeePhoto;
    }

    public void setEmployeePhoto(String employeePhoto) {
        this.employeePhoto = employeePhoto;
    }

    public List<Position> getPositions() {
        return positions;
    }

    public void setPositions(List<Position> positions) {
        this.positions = positions;
    }
}
