package com.w2w.api.position.dto;

import java.util.List;

public class PositionsResponse {
    private List<PositionDto> positions;
    private List<PositionGroupDto> groups;

    public PositionsResponse() {}

    public PositionsResponse(List<PositionDto> positions, List<PositionGroupDto> groups) {
        this.positions = positions;
        this.groups = groups;
    }

    public List<PositionDto> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionDto> positions) {
        this.positions = positions;
    }

    public List<PositionGroupDto> getGroups() {
        return groups;
    }

    public void setGroups(List<PositionGroupDto> groups) {
        this.groups = groups;
    }
}
