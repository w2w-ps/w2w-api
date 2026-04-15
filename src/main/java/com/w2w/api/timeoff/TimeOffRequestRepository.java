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

    @Query("""
            SELECT request
            FROM TimeOffRequest request
            WHERE request.companyId = :companyId
              AND (:employeeId IS NULL OR request.employeeId = :employeeId)
              AND (:status IS NULL OR UPPER(request.status) = UPPER(:status))
              AND (:startDate IS NULL OR request.endDate >= :startDate)
              AND (:endDate IS NULL OR request.startDate <= :endDate)
            ORDER BY request.requestedAt DESC, request.requestId DESC
            """)
    List<TimeOffRequest> findRequests(
            @Param("companyId") Integer companyId,
            @Param("employeeId") Integer employeeId,
            @Param("status") String status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<TimeOffRequest> findByRequestIdAndCompanyId(Integer requestId, Integer companyId);
}