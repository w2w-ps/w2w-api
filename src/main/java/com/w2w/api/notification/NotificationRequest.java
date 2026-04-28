package com.w2w.api.notification;

import java.util.List;

public record NotificationRequest(
    String task,
    Integer leaveRequestId,
    Integer scheduleId,
    Integer shiftId,
    List<Integer> positionIds,
    Integer companyId
) {}
