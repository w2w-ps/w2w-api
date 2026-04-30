package com.w2w.api.timeoff;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.timeoff.dto.ApproveTimeOffRequest;
import com.w2w.api.timeoff.dto.CreateTimeOffRequest;
import com.w2w.api.timeoff.dto.TimeOffSummary;
import com.w2w.api.timeoff.dto.TimeOffRequestsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.w2w.api.notification.NotificationProducer;
import com.w2w.api.notification.NotificationRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class TimeOffService {
    private static final String APPROVED = "APPROVED";
    private static final String CANCELLED = "CANCELLED";
    private static final String DECLINED = "DECLINED";
    private static final String PENDING = "PENDING";
    private static final String TIME_OFF_REQUEST_NOT_FOUND = "Time off request not found";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH);
    private static final Set<String> SCHEDULING_BLOCKING_STATUSES = Set.of(PENDING, APPROVED);

    private final TimeOffRequestRepository timeOffRequestRepository;
    private final NotificationProducer notificationProducer;

    public TimeOffService(TimeOffRequestRepository timeOffRequestRepository, NotificationProducer notificationProducer) {
        this.timeOffRequestRepository = timeOffRequestRepository;
        this.notificationProducer = notificationProducer;
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
        List<TimeOffSummary> requests = timeOffRequestRepository.findRequests(
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

    @Transactional(readOnly = true)
    public List<TimeOffRequest> findBlockingTimeOff(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (employeeId == null || startDate == null || endDate == null) {
            return List.of();
        }

        Integer companyId = CurrentTenant.requireCurrentTenant();
        return timeOffRequestRepository.findRequests(companyId, employeeId, null, startDate, endDate)
                .stream()
                .filter(this::isSchedulingBlocking)
                .toList();
    }

    @Transactional
    @PreAuthorize("@timeOffPolicy.canCreateForEmployee(#request.employeeId(), authentication)")
    public TimeOffSummary createTimeOffRequest(CreateTimeOffRequest request) {
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
        entity.setStatus(PENDING);
        entity.setComments(request.comments());

        TimeOffRequest saved = timeOffRequestRepository.save(entity);
        notificationProducer.sendNotification(new NotificationRequest("LEAVE_CREATE", saved.getRequestId(), null, null, null, companyId));
        return toSummary(saved);
    }

    @Transactional
    @PreAuthorize("@timeOffPolicy.canApproveTimeOffRequest(authentication)")
    public TimeOffSummary approveTimeOffRequest(Integer requestId, ApproveTimeOffRequest request) {
        Integer companyId = CurrentTenant.requireCurrentTenant();

        TimeOffRequest timeOffRequest = timeOffRequestRepository.findByRequestIdAndCompanyId(requestId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, TIME_OFF_REQUEST_NOT_FOUND));

        if (!canApprove(timeOffRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending time off requests can be approved or declined");
        }

        String newStatus = switch (request.action()) {
            case APPROVE -> APPROVED;
            case DECLINE -> DECLINED;
        };

        timeOffRequest.setStatus(newStatus);
        if (request.managerComments() != null && !request.managerComments().trim().isEmpty()) {
            timeOffRequest.setComments(timeOffRequest.getComments() != null 
                ? timeOffRequest.getComments() + "\n\nManager: " + request.managerComments().trim()
                : "Manager: " + request.managerComments().trim());
        }

        TimeOffRequest saved = timeOffRequestRepository.save(timeOffRequest);
        String task = request.action() == ApproveTimeOffRequest.Action.APPROVE ? "LEAVE_APPROVE" : "LEAVE_DECLINE";
        notificationProducer.sendNotification(new NotificationRequest(task, saved.getRequestId(), null, null, null, companyId));
        return toSummary(saved);
    }

    @Transactional
    @PreAuthorize("@timeOffPolicy.canCancelTimeOffRequest(#requestId, authentication)")
    public void cancelTimeOffRequest(Integer requestId) {
        Integer companyId = CurrentTenant.requireCurrentTenant();

        TimeOffRequest timeOffRequest = timeOffRequestRepository.findByRequestIdAndCompanyId(requestId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, TIME_OFF_REQUEST_NOT_FOUND));

        if (!canCancel(timeOffRequest.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending time off requests can be cancelled");
        }

        timeOffRequest.setStatus(CANCELLED);
        timeOffRequestRepository.save(timeOffRequest);
        notificationProducer.sendNotification(new NotificationRequest("LEAVE_CANCEL", requestId, null, null, null, companyId));
    }

    private TimeOffSummary toSummary(TimeOffRequest request) {
        return new TimeOffSummary(
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
            case PENDING, APPROVED, DECLINED, CANCELLED -> normalized;
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
        return status != null && PENDING.equalsIgnoreCase(status);
    }

    private boolean canApprove(String status) {
        return status != null && PENDING.equalsIgnoreCase(status);
    }

    private boolean isSchedulingBlocking(TimeOffRequest request) {
        if (request.getStatus() == null) {
            return false;
        }

        return SCHEDULING_BLOCKING_STATUSES.contains(request.getStatus().trim().toUpperCase(Locale.ENGLISH));
    }
}
