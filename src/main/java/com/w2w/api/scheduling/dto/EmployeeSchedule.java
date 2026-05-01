package com.w2w.api.scheduling.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmployeeSchedule {
    private static final DateTimeFormatter DAY_BUCKET_FORMATTER = DateTimeFormatter.ofPattern("EEEE MMM-dd", Locale.ENGLISH);

    private Integer employeeId;
    private String firstName;
    private String lastName;
    private List<String> phones;
    private Map<Integer, DayShiftBucket> weeklyShifts = new LinkedHashMap<>();
    private BigDecimal totalHours;
    private int shiftCount;
    private String employmentType;
    private Integer empTypeId;
    private String alertDate;
    private String publishedStage;

    public EmployeeSchedule() {}

    public EmployeeSchedule(
            Integer employeeId,
            String firstName,
            String lastName,
            List<String> phones,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phones = phones;
        this.totalHours = BigDecimal.ZERO.setScale(2);
        this.shiftCount = 0;
        initializeWeeklyShifts(startDate, endDate);
    }

    private void initializeWeeklyShifts(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return;
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
        for (int i = 0; i <= totalDays; i++) {
            LocalDate bucketDate = startDate.plusDays(i);
            weeklyShifts.put(i, new DayShiftBucket(bucketDate.format(DAY_BUCKET_FORMATTER), new ArrayList<>()));
        }
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
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

    public List<String> getPhones() {
        return phones;
    }

    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    public Map<Integer, DayShiftBucket> getWeeklyShifts() {
        return weeklyShifts;
    }

    public void setWeeklyShifts(Map<Integer, DayShiftBucket> weeklyShifts) {
        this.weeklyShifts = weeklyShifts;
    }

    public void addShiftToDay(int dayIndex, String date, ShiftSummary shift) {
        DayShiftBucket bucket = weeklyShifts.computeIfAbsent(
                dayIndex,
                ignored -> new DayShiftBucket(date, new ArrayList<>())
        );

        bucket.shifts().add(shift);
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public int getShiftCount() {
        return shiftCount;
    }

    public void setShiftCount(int shiftCount) {
        this.shiftCount = shiftCount;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Integer getEmpTypeId() {
        return empTypeId;
    }

    public void setEmpTypeId(Integer empTypeId) {
        this.empTypeId = empTypeId;
    }

    public String getAlertDate() {
        return alertDate;
    }

    public void setAlertDate(String alertDate) {
        this.alertDate = alertDate;
    }

    public String getPublishedStage() {
        return publishedStage;
    }

    public void setPublishedStage(String publishedStage) {
        this.publishedStage = publishedStage;
    }
}
