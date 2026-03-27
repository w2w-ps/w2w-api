package com.w2w.api.position.dto;

import java.util.List;

public class PositionGroupDto {
    private Integer id;
    private String name;
    private List<PositionDto> positions;

    public PositionGroupDto() {}

    public PositionGroupDto(Integer id, String name, List<PositionDto> positions) {
        this.id = id;
        this.name = name;
        this.positions = positions;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<PositionDto> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionDto> positions) {
        this.positions = positions;
    }
}
