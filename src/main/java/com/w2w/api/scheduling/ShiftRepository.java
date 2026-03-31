package com.w2w.api.scheduling;

import com.w2w.api.scheduling.dto.ShiftDetailsProjection;
import com.w2w.api.scheduling.model.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Integer> {
    @Query(value = """
            SELECT
                se.transaction_id AS transactionId,
                se.employee_id AS employeeId,
                se.company_id AS companyId,
                se.description AS description,
                sc.start_date AS date,
                se.start_time AS startTime,
                se.end_time AS endTime,
                se.duration AS duration,
                se.is_overnight AS isOvernight,
                sk.description AS position,
                cat.description AS category,
                se.color AS color
            FROM scheduled_employee se
            LEFT JOIN schedule sc ON se.schedule_id = sc.schedule_id
            LEFT JOIN skill sk ON se.required_skill_id = sk.skill_id
            LEFT JOIN category cat ON se.category_id = cat.category_id
            WHERE se.transaction_id = :transactionId
            """, nativeQuery = true)
    Optional<ShiftDetailsProjection> findShiftDetailsByTransactionId(@Param("transactionId") Integer transactionId);
}
