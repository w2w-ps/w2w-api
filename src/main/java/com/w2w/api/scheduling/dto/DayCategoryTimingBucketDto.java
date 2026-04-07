package com.w2w.api.scheduling.dto;

import java.time.LocalDate;
import java.util.List;

public record DayCategoryTimingBucketDto(
        LocalDate date,
        List<CategoryTimingBucketDto> categories
) {
}
