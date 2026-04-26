package com.w2w.api.timeoff;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeOffRequestRepository extends JpaRepository<TimeOffRequest, Integer> {

    @Query(value = """
            SELECT *
            FROM time_off_request request
            WHERE request.company_id = :companyId
              AND (CAST(:employeeId AS integer) IS NULL OR request.employee_id = :employeeId)
              AND (CAST(:status AS varchar) IS NULL OR request.status = :status)
              AND (CAST(:startDate AS date) IS NULL OR request.end_date >= CAST(:startDate AS date))
              AND (CAST(:endDate AS date) IS NULL OR request.start_date <= CAST(:endDate AS date))
            ORDER BY request.requested_at DESC, request.request_id DESC
            """, nativeQuery = true)
    List<TimeOffRequest> findRequests(
            @Param("companyId") Integer companyId,
            @Param("employeeId") Integer employeeId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<TimeOffRequest> findByRequestIdAndCompanyId(Integer requestId, Integer companyId);
}
