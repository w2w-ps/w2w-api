package com.w2w.api.position.dto;

import java.util.List;

public record PositionsResponse(List<PositionSummary> positions, List<PositionGroupSummary> groups) {
}
