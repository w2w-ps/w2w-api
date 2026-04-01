package com.w2w.api.scheduling.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Record for updating shift details and handling reassignment operations.
 * Includes shift identifiers and details for modification.
 *
 * @param shiftId         The unique identifier of the shift to be updated or reassigned.
 * @param employeeId      The identifier of the employee assigned to the shift.
 * @param description     A description of the shift.
 * @param startTime       The start time of the shift.
 * @param endTime         The end time of the shift.
 * @param position        The position or skill required for the shift.
 * @param category        The category of the shift.
 * @param color           A color code associated with the shift.
 * @param date            The date of the shift.
 * @param duration        The duration of the shift in hours.
 */
public record UpdateShiftRequest(
        Integer shiftId,
        Integer employeeId,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime startTime,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "h:mma") LocalTime endTime,
        Integer position,
        Integer category,
        String color,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
        Float duration
) {
}
