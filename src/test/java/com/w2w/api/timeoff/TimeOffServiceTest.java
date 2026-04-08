package com.w2w.api.timeoff;

import com.w2w.api.timeoff.dto.TimeOffRequestsResponse;
import com.w2w.api.timeoff.dto.CreateTimeOffRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

        TimeOffRequestsResponse response = timeOffService.getTimeOffRequests(1, null, "all", null, null);

        assertEquals(1, response.timeOffRequests().size());
        assertEquals("8am to 11:59pm", response.timeOffRequests().getFirst().endDateTimes());
        assertEquals("Repeats for 2 Weeks", response.timeOffRequests().getFirst().repeatSummary());
        assertEquals(true, response.timeOffRequests().getFirst().canCancel());
        assertEquals(LocalDateTime.of(2024, 9, 27, 7, 17), response.timeOffRequests().getFirst().requestedAt());
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

        TimeOffRequestsResponse response = timeOffService.getTimeOffRequests(1, 11, "all", null, null);

        assertEquals("Jun 5, 2023", response.timeOffRequests().getFirst().endDateTimes());
        assertNull(response.timeOffRequests().getFirst().repeatSummary());
        assertEquals(false, response.timeOffRequests().getFirst().canCancel());
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
                1,
                11,
                LocalDate.of(2026, 4, 22),
                true,
                2,
                null,
                null,
                null,
                "Optional comment"
        );

        com.w2w.api.config.TenantContext.setCurrentTenant(1);
        try {
            assertEquals("Apr 22, 2026 to Apr 23, 2026", timeOffService.createTimeOffRequest(request).endDateTimes());
        } finally {
            com.w2w.api.config.TenantContext.clear();
        }

        verify(timeOffRequestRepository).save(any(TimeOffRequest.class));
    }
}