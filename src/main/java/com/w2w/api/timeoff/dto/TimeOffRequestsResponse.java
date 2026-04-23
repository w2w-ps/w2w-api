package com.w2w.api.timeoff.dto;

import java.util.List;

public record TimeOffRequestsResponse(List<TimeOffSummary> timeOffRequests) {
}