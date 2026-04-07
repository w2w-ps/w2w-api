package com.w2w.api.positiongroup.dto;

import com.w2w.api.position.dto.PositionSummary;
import java.util.List;

public record PositionGroupSummary(Integer id, String name, List<PositionSummary> positions) {
}
