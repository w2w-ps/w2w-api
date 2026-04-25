package com.w2w.api.timeoff;

import com.w2w.api.config.TenantContext;
import com.w2w.api.timeoff.dto.ApproveTimeOffRequest;
import com.w2w.api.timeoff.dto.CreateTimeOffRequest;
import com.w2w.api.timeoff.dto.TimeOffRequestsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TimeOffServiceTest {
    private TimeOffRequestRepository timeOffRequestRepository;
    private TimeOffService timeOffService;

    @BeforeEach
    void setUp() {
        timeOffRequestRepository = Mockito.mock(TimeOffRequestRepository.class);
        timeOffService = new TimeOffService(timeOffRequestRepository);
    }

    @Test
    void getTimeOffRequestsMapsDisplayFieldsAndRepeatSummary() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(1001);
        request.setCompanyId(1);
        request.setEmployeeId(10);
        request.setStartDate(LocalDate.of(2024, 11, 15));
        request.setEndDate(LocalDate.of(2024, 11, 15));
        request.setStartTime(LocalTime.of(8, 0));
        request.setEndTime(LocalTime.of(23, 59));
        request.setRequestedAt(LocalDateTime.of(2024, 9, 27, 7, 17));
        request.setStatus("PENDING");
        request.setComments("Vacation Time");
        request.setRepeatCount(2);

        when(timeOffRequestRepository.findRequests(1, null, null, null, null)).thenReturn(List.of(request));

        TenantContext.setCurrentTenant(1);
        try {
            TimeOffRequestsResponse response = timeOffService.getTimeOffRequests(null, "all", null, null);

            assertEquals(1, response.timeOffRequests().size());
            assertEquals("8am to 11:59pm", response.timeOffRequests().getFirst().endDateTimes());
            assertEquals("Repeats for 2 Weeks", response.timeOffRequests().getFirst().repeatSummary());
            assertEquals(true, response.timeOffRequests().getFirst().canCancel());
            assertEquals(LocalDateTime.of(2024, 9, 27, 7, 17), response.timeOffRequests().getFirst().requestedAt());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void getTimeOffRequestsTreatsAllAsUnfilteredAndSupportsDateOnlyDisplay() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(1002);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStartDate(LocalDate.of(2023, 6, 5));
        request.setEndDate(LocalDate.of(2023, 6, 5));
        request.setRequestedAt(LocalDateTime.of(2022, 10, 18, 13, 41));
        request.setStatus("APPROVED");
        request.setComments("Personal Day");

        when(timeOffRequestRepository.findRequests(1, 11, null, null, null)).thenReturn(List.of(request));

        TenantContext.setCurrentTenant(1);
        try {
            TimeOffRequestsResponse response = timeOffService.getTimeOffRequests(11, "all", null, null);

            assertEquals("Jun 5, 2023", response.timeOffRequests().getFirst().endDateTimes());
            assertNull(response.timeOffRequests().getFirst().repeatSummary());
            assertEquals(false, response.timeOffRequests().getFirst().canCancel());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void findBlockingTimeOff_returnsOnlyPendingAndApprovedStatuses() {
        TimeOffRequest pending = timeOffRequest(11, LocalDate.of(2026, 4, 21), "PENDING");
        TimeOffRequest approved = timeOffRequest(11, LocalDate.of(2026, 4, 22), "APPROVED");
        TimeOffRequest declined = timeOffRequest(11, LocalDate.of(2026, 4, 23), "DECLINED");
        TimeOffRequest cancelled = timeOffRequest(11, LocalDate.of(2026, 4, 24), "CANCELLED");

        when(timeOffRequestRepository.findRequests(
                1,
                11,
                null,
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 4, 24)
        )).thenReturn(List.of(pending, approved, declined, cancelled));

        TenantContext.setCurrentTenant(1);
        try {
            List<TimeOffRequest> results = timeOffService.findBlockingTimeOff(
                    11,
                    LocalDate.of(2026, 4, 21),
                    LocalDate.of(2026, 4, 24)
            );

            assertEquals(List.of(pending, approved), results);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void findBlockingTimeOff_filtersByEmployeeAndDateRangeAndPreservesFullDayAndTimedRows() {
        TimeOffRequest fullDay = timeOffRequest(11, LocalDate.of(2026, 4, 21), "APPROVED");
        fullDay.setFullDay(true);
        fullDay.setEndDate(LocalDate.of(2026, 4, 22));

        TimeOffRequest timed = timeOffRequest(11, LocalDate.of(2026, 4, 23), "PENDING");
        timed.setFullDay(false);
        timed.setStartTime(LocalTime.of(9, 0));
        timed.setEndTime(LocalTime.of(12, 0));
        timed.setRepeatCount(3);

        when(timeOffRequestRepository.findRequests(
                1,
                11,
                null,
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 4, 23)
        )).thenReturn(List.of(fullDay, timed));

        TenantContext.setCurrentTenant(1);
        try {
            List<TimeOffRequest> results = timeOffService.findBlockingTimeOff(
                    11,
                    LocalDate.of(2026, 4, 21),
                    LocalDate.of(2026, 4, 23)
            );

            assertEquals(2, results.size());
            assertSame(fullDay, results.get(0));
            assertSame(timed, results.get(1));
            assertTrue(Boolean.TRUE.equals(results.get(0).getFullDay()));
            assertEquals(LocalDate.of(2026, 4, 22), results.get(0).getEndDate());
            assertEquals(LocalTime.of(9, 0), results.get(1).getStartTime());
            assertEquals(LocalTime.of(12, 0), results.get(1).getEndTime());
            assertEquals(3, results.get(1).getRepeatCount());
        } finally {
            TenantContext.clear();
        }

        verify(timeOffRequestRepository).findRequests(
                1,
                11,
                null,
                LocalDate.of(2026, 4, 21),
                LocalDate.of(2026, 4, 23)
        );
    }

    @Test
    void findBlockingTimeOff_missingRequiredArgumentsReturnsEmpty() {
        TenantContext.setCurrentTenant(1);
        try {
            assertTrue(timeOffService.findBlockingTimeOff(null, LocalDate.of(2026, 4, 21), LocalDate.of(2026, 4, 21)).isEmpty());
            assertTrue(timeOffService.findBlockingTimeOff(11, null, LocalDate.of(2026, 4, 21)).isEmpty());
            assertTrue(timeOffService.findBlockingTimeOff(11, LocalDate.of(2026, 4, 21), null).isEmpty());
        } finally {
            TenantContext.clear();
        }

        verifyNoInteractions(timeOffRequestRepository);
    }

    @Test
    void createTimeOffRequestPersistsPendingFullDayRequest() {
        TimeOffRequest saved = new TimeOffRequest();
        saved.setRequestId(2001);
        saved.setCompanyId(1);
        saved.setEmployeeId(11);
        saved.setStartDate(LocalDate.of(2026, 4, 22));
        saved.setEndDate(LocalDate.of(2026, 4, 23));
        saved.setFullDay(true);
        saved.setDayCount(2);
        saved.setRequestedAt(LocalDateTime.of(2026, 4, 8, 10, 0));
        saved.setStatus("PENDING");
        saved.setComments("Optional comment");
        saved.setRepeatCount(1);

        when(timeOffRequestRepository.save(any(TimeOffRequest.class))).thenReturn(saved);

        CreateTimeOffRequest request = new CreateTimeOffRequest(
                11,
                LocalDate.of(2026, 4, 22),
                LocalDate.of(2026, 4, 23),
                true,
                null,
                null,
                null,
                "Optional comment"
        );

        TenantContext.setCurrentTenant(1);
        try {
            assertEquals("Apr 22, 2026 to Apr 23, 2026", timeOffService.createTimeOffRequest(request).endDateTimes());
        } finally {
            TenantContext.clear();
        }

        verify(timeOffRequestRepository).save(any(TimeOffRequest.class));
    }

    @Test
    void cancelTimeOffRequestMarksPendingRequestCancelled() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(3001);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStatus("PENDING");

        when(timeOffRequestRepository.findByRequestIdAndCompanyId(3001, 1)).thenReturn(Optional.of(request));

        TenantContext.setCurrentTenant(1);
        try {
            timeOffService.cancelTimeOffRequest(3001);
        } finally {
            TenantContext.clear();
        }

        assertEquals("CANCELLED", request.getStatus());
        verify(timeOffRequestRepository).save(request);
    }

    @Test
    void cancelTimeOffRequestRejectsNonPendingRequests() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(3002);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStatus("APPROVED");

        when(timeOffRequestRepository.findByRequestIdAndCompanyId(3002, 1)).thenReturn(Optional.of(request));

        TenantContext.setCurrentTenant(1);
        try {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> timeOffService.cancelTimeOffRequest(3002));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Only pending time off requests can be cancelled", exception.getReason());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void cancelTimeOffRequestRejectsCompanyMismatch() {
        TenantContext.setCurrentTenant(1);
        try {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> timeOffService.cancelTimeOffRequest(3003));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Time off request not found", exception.getReason());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void approveTimeOffRequestApprovesPendingRequest() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(4001);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStatus("PENDING");
        request.setComments("Original comment");

        when(timeOffRequestRepository.findByRequestIdAndCompanyId(4001, 1)).thenReturn(Optional.of(request));
        when(timeOffRequestRepository.save(any(TimeOffRequest.class))).thenReturn(request);

        ApproveTimeOffRequest approveRequest = new ApproveTimeOffRequest(
                ApproveTimeOffRequest.Action.APPROVE,
                "Approved by manager"
        );

        TenantContext.setCurrentTenant(1);
        try {
            var result = timeOffService.approveTimeOffRequest(4001, approveRequest);
            assertEquals("APPROVED", request.getStatus());
            assertEquals("Original comment\n\nManager: Approved by manager", request.getComments());
            assertEquals(4001, result.requestId());
        } finally {
            TenantContext.clear();
        }

        verify(timeOffRequestRepository).save(request);
    }

    @Test
    void approveTimeOffRequestDeclinesPendingRequest() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(4002);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStatus("PENDING");
        request.setComments(null);

        when(timeOffRequestRepository.findByRequestIdAndCompanyId(4002, 1)).thenReturn(Optional.of(request));
        when(timeOffRequestRepository.save(any(TimeOffRequest.class))).thenReturn(request);

        ApproveTimeOffRequest approveRequest = new ApproveTimeOffRequest(
                ApproveTimeOffRequest.Action.DECLINE,
                "Insufficient coverage"
        );

        TenantContext.setCurrentTenant(1);
        try {
            var result = timeOffService.approveTimeOffRequest(4002, approveRequest);
            assertEquals("DECLINED", request.getStatus());
            assertEquals("Manager: Insufficient coverage", request.getComments());
            assertEquals(4002, result.requestId());
        } finally {
            TenantContext.clear();
        }

        verify(timeOffRequestRepository).save(request);
    }

    @Test
    void approveTimeOffRequestApprovesWithoutManagerComments() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(4003);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStatus("PENDING");
        request.setComments("Original comment");

        when(timeOffRequestRepository.findByRequestIdAndCompanyId(4003, 1)).thenReturn(Optional.of(request));
        when(timeOffRequestRepository.save(any(TimeOffRequest.class))).thenReturn(request);

        ApproveTimeOffRequest approveRequest = new ApproveTimeOffRequest(
                ApproveTimeOffRequest.Action.APPROVE,
                null
        );

        TenantContext.setCurrentTenant(1);
        try {
            var result = timeOffService.approveTimeOffRequest(4003, approveRequest);
            assertEquals("APPROVED", request.getStatus());
            assertEquals("Original comment", request.getComments());
            assertEquals(4003, result.requestId());
        } finally {
            TenantContext.clear();
        }

        verify(timeOffRequestRepository).save(request);
    }

    @Test
    void approveTimeOffRequestRejectsNonPendingRequests() {
        TimeOffRequest request = new TimeOffRequest();
        request.setRequestId(4004);
        request.setCompanyId(1);
        request.setEmployeeId(11);
        request.setStatus("APPROVED");

        when(timeOffRequestRepository.findByRequestIdAndCompanyId(4004, 1)).thenReturn(Optional.of(request));

        ApproveTimeOffRequest approveRequest = new ApproveTimeOffRequest(
                ApproveTimeOffRequest.Action.DECLINE,
                "Should not work"
        );

        TenantContext.setCurrentTenant(1);
        try {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> timeOffService.approveTimeOffRequest(4004, approveRequest));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Only pending time off requests can be approved or declined", exception.getReason());
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void approveTimeOffRequestRejectsCompanyMismatch() {
        TenantContext.setCurrentTenant(1);
        try {
            ApproveTimeOffRequest approveRequest = new ApproveTimeOffRequest(
                    ApproveTimeOffRequest.Action.APPROVE,
                    "Should not work"
            );
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> timeOffService.approveTimeOffRequest(4005, approveRequest));
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            assertEquals("Time off request not found", exception.getReason());
        } finally {
            TenantContext.clear();
        }
    }

    private TimeOffRequest timeOffRequest(Integer employeeId, LocalDate startDate, String status) {
        TimeOffRequest request = new TimeOffRequest();
        request.setEmployeeId(employeeId);
        request.setStartDate(startDate);
        request.setEndDate(startDate);
        request.setStatus(status);
        return request;
    }
}
