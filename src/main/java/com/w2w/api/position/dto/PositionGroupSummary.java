package com.w2w.api.position.dto;

import java.util.List;

public record PositionGroupSummary(Integer id, String name, List<PositionSummary> positions) {
}
