package com.w2w.api.position.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * A summary DTO for Position information.
 * Represents a concise view of a position, primarily for listing purposes.
 */
public record PositionSummary(
    /**
      The unique identifier of the position.
     */
    @JsonAlias("id")
    Integer positionId,
    /**
     * The description/name of the position.
     */
    @JsonAlias("name")
    String description
) {
}
