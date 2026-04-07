package com.w2w.api.scheduling.dto;

import com.w2w.api.position.dto.PositionSummary;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmployeeSchedule {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private List<String> phones;
    private List<PositionSummary> availablePositions;
    private Map<Integer, DayShiftBucket> weeklyShifts = new LinkedHashMap<>();
    private double totalHours;
    private int shiftCount;

    public EmployeeSchedule() {}

    public EmployeeSchedule(
            Integer employeeId,
            String firstName,
            String lastName,
            List<String> phones,
            List<PositionSummary> availablePositions,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phones = phones;
        this.availablePositions = availablePositions;
        this.totalHours = 0.0;
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
            weeklyShifts.put(i, new DayShiftBucket(bucketDate, new ArrayList<>()));
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

    public List<PositionSummary> getAvailablePositions() {
        return availablePositions;
    }

    public void setAvailablePositions(List<PositionSummary> availablePositions) {
        this.availablePositions = availablePositions;
    }

    public Map<Integer, DayShiftBucket> getWeeklyShifts() {
        return weeklyShifts;
    }

    public void setWeeklyShifts(Map<Integer, DayShiftBucket> weeklyShifts) {
        this.weeklyShifts = weeklyShifts;
    }

    public void addShiftToDay(int dayIndex, LocalDate date, ShiftSummary shift) {
        DayShiftBucket bucket = weeklyShifts.computeIfAbsent(
                dayIndex,
                ignored -> new DayShiftBucket(date, new ArrayList<>())
        );

        bucket.shifts().add(shift);
    }

    public double getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(double totalHours) {
        this.totalHours = totalHours;
    }

    public int getShiftCount() {
        return shiftCount;
    }

    public void setShiftCount(int shiftCount) {
        this.shiftCount = shiftCount;
    }
}
