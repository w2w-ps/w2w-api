package com.w2w.api.notification;

import java.util.List;

public record NotificationRequest(
    List<String> userIds,
    String type,
    String message,
    Integer companyId
) {}
