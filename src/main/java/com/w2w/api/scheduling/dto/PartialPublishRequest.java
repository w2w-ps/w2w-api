package com.w2w.api.scheduling.dto;

import java.util.List;

public record PartialPublishRequest(
    Integer scheduleId,
    List<Integer> positionIds
) {}
