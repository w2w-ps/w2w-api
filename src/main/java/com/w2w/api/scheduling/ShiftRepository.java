package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.DailyHoursShiftProjection;
import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Integer> {
    @Query(value = """
            SELECT
                se.shift_id AS shiftId,
                se.employee_id AS employeeId,
                se.company_id AS companyId,
                se.description AS description,
                sc.start_date AS date,
                se.start_time AS startTime,
                se.end_time AS endTime,
                se.duration AS duration,
                se.is_overnight AS isOvernight,
                sk.description AS position,
                cat.short_desc AS category,
                se.color AS color
            FROM scheduled_employee se
            LEFT JOIN schedule sc ON se.schedule_id = sc.schedule_id
            LEFT JOIN position sk ON se.required_position_id = sk.position_id
            LEFT JOIN category cat ON se.category_id = cat.category_id
            WHERE se.shift_id = :shiftId
              AND se.is_deleted = false
            """, nativeQuery = true)
    Optional<ShiftDetailsProjection> findShiftDetailsByShiftId(@Param("shiftId") Integer shiftId);

    @Query(value = """
            SELECT
                se.shift_id AS shiftId,
                se.employee_id AS employeeId,
                se.company_id AS companyId,
                se.description AS description,
                sc.start_date AS date,
                se.start_time AS startTime,
                se.end_time AS endTime,
                se.duration AS duration,
                se.is_overnight AS isOvernight,
                sk.description AS position,
                cat.short_desc AS category,
                se.color AS color
            FROM scheduled_employee se
            LEFT JOIN schedule sc ON se.schedule_id = sc.schedule_id
            LEFT JOIN position sk ON se.required_position_id = sk.position_id
            LEFT JOIN category cat ON se.category_id = cat.category_id
            WHERE se.shift_id = :shiftId
              AND se.company_id = :companyId
              AND se.is_deleted = false
            """, nativeQuery = true)
    Optional<ShiftDetailsProjection> findShiftDetailsByShiftIdAndCompanyId(@Param("shiftId") Integer shiftId, @Param("companyId") Integer companyId);

    Optional<Shift> findByShiftIdAndCompanyId(Integer shiftId, Integer companyId);

    @Query(value = """
            SELECT se.*
            FROM scheduled_employee se
            JOIN schedule sc ON se.schedule_id = sc.schedule_id
            WHERE se.company_id = :companyId
              AND se.employee_id = :employeeId
              AND sc.start_date = :date
              AND se.is_deleted = false
              AND (:excludedShiftId IS NULL OR se.shift_id <> :excludedShiftId)
            """, nativeQuery = true)
    List<Shift> findActiveByCompanyIdAndEmployeeIdAndDateExcludingShiftId(
            @Param("companyId") Integer companyId,
            @Param("employeeId") Integer employeeId,
            @Param("date") LocalDate date,
            @Param("excludedShiftId") Integer excludedShiftId
    );

    @Query(value = """
            SELECT
                se.shift_id AS shiftId,
                sc.start_date AS date,
                se.start_time AS startTime,
                se.end_time AS endTime,
                se.duration AS duration
            FROM scheduled_employee se
            JOIN schedule sc ON se.schedule_id = sc.schedule_id
            WHERE se.company_id = :companyId
              AND se.employee_id = :employeeId
              AND sc.start_date BETWEEN :startDate AND :endDate
              AND se.is_deleted = false
              AND (:excludedShiftId IS NULL OR se.shift_id <> :excludedShiftId)
            """, nativeQuery = true)
    List<DailyHoursShiftProjection> findActiveDailyHoursShiftsByCompanyIdAndEmployeeIdAndDateRangeExcludingShiftId(
            @Param("companyId") Integer companyId,
            @Param("employeeId") Integer employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludedShiftId") Integer excludedShiftId
    );
}
