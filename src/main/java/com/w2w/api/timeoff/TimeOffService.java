package com.w2w.api.timeoff;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.timeoff.dto.CreateTimeOffRequest;
import com.w2w.api.timeoff.dto.TimeOffRequestSummary;
import com.w2w.api.timeoff.dto.TimeOffRequestsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
public class TimeOffService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);

    private final TimeOffRequestRepository timeOffRequestRepository;

    public TimeOffService(TimeOffRequestRepository timeOffRequestRepository) {
        this.timeOffRequestRepository = timeOffRequestRepository;
    }

    @Transactional(readOnly = true)
    public TimeOffRequestsResponse getTimeOffRequests(
            Integer employeeId,
            String status,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Integer companyId = CurrentTenant.requireCurrentTenant();
        String normalizedStatus = normalizeStatus(status);
        List<TimeOffRequestSummary> requests = timeOffRequestRepository.findRequests(
                        companyId,
                        employeeId,
                        normalizedStatus,
                        startDate,
                        endDate
                )
                .stream()
                .map(this::toSummary)
                .toList();

        return new TimeOffRequestsResponse(requests);
    }

    @Transactional
    public TimeOffRequestSummary createTimeOffRequest(CreateTimeOffRequest request) {
        Integer companyId = CurrentTenant.requireCurrentTenant();

        validateCreateRequest(request);

        TimeOffRequest entity = new TimeOffRequest();
        entity.setCompanyId(companyId);
        entity.setEmployeeId(request.employeeId());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setFullDay(Boolean.TRUE.equals(request.fullDay()));
        
        // Calculate dayCount from date range
        long calculatedDayCount = ChronoUnit.DAYS.between(request.startDate(), request.endDate()) + 1;
        entity.setDayCount((int) calculatedDayCount);
        
        if (Boolean.TRUE.equals(request.fullDay())) {
            entity.setStartTime(null);
            entity.setEndTime(null);
            entity.setRepeatCount(1);
        } else {
            entity.setDayCount(1);
            entity.setStartTime(request.startTime());
            entity.setEndTime(request.endTime());
            entity.setRepeatCount(request.repeatCount());
        }
        entity.setRequestedAt(LocalDateTime.now());
        entity.setStatus("PENDING");
        entity.setComments(request.comments());

        return toSummary(timeOffRequestRepository.save(entity));
    }

    @Transactional
    public void cancelTimeOffRequest(Integer requestId) {
        Integer companyId = CurrentTenant.requireCurrentTenant();

        TimeOffRequest request = timeOffRequestRepository.findByRequestIdAndCompanyId(requestId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time off request not found"));

        if (!canCancel(request.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending time off requests can be cancelled");
        }

        request.setStatus("CANCELLED");
        timeOffRequestRepository.save(request);
    }

    private TimeOffRequestSummary toSummary(TimeOffRequest request) {
        return new TimeOffRequestSummary(
                request.getRequestId(),
                request.getEmployeeId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStartTime(),
                request.getEndTime(),
                buildEndDateTimesLabel(request),
                request.getRequestedAt(),
                request.getStatus(),
                request.getComments(),
                buildRepeatSummary(request.getRepeatCount()),
                canCancel(request.getStatus())
        );
    }

    private void validateCreateRequest(CreateTimeOffRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be before or equal to endDate");
        }
        
        if (Boolean.TRUE.equals(request.fullDay())) {
            // No dayCount validation needed - calculated from date range
        } else {
            if (request.startTime() == null || request.endTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startTime and endTime are required for partial requests");
            }
            if (request.repeatCount() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "repeatCount is required for partial requests");
            }
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            return null;
        }

        String normalized = status.trim().toUpperCase(Locale.ENGLISH);
        return switch (normalized) {
            case "PENDING", "APPROVED", "DECLINED", "CANCELLED" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported time off status filter");
        };
    }

    private String buildEndDateTimesLabel(TimeOffRequest request) {
        if (Boolean.TRUE.equals(request.getFullDay())) {
            if (request.getDayCount() != null && request.getDayCount() > 1 && request.getStartDate() != null && request.getEndDate() != null) {
                return DATE_FORMATTER.format(request.getStartDate()) + " to " + DATE_FORMATTER.format(request.getEndDate());
            }

            return request.getStartDate() != null ? DATE_FORMATTER.format(request.getStartDate()) : null;
        }

        if (request.getStartTime() != null && request.getEndTime() != null) {
            return formatTime(request.getStartTime()) + " to " + formatTime(request.getEndTime());
        }

        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();
        return endDate != null ? DATE_FORMATTER.format(endDate) : null;
    }

    private String buildRepeatSummary(Integer repeatCount) {
        if (repeatCount == null || repeatCount < 2) {
            return null;
        }

        return "Repeats for " + repeatCount + " " + (repeatCount == 1 ? "Week" : "Weeks");
    }

    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("h:mma", Locale.ENGLISH)).replace(":00", "").toLowerCase(Locale.ENGLISH);
    }

    private boolean canCancel(String status) {
        return status != null && "PENDING".equalsIgnoreCase(status);
    }
}