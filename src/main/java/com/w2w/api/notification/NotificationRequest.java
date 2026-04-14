package com.w2w.api.notification;

import java.util.List;

public record NotificationRequest(
    String task,
    Integer leaveRequestId,
    Integer scheduleId
) {}
