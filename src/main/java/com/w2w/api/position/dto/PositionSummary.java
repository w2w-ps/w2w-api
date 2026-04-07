package com.w2w.api.position.dto;

import java.time.LocalDateTime;

/**
 * A summary DTO for Position information.
 * Represents a concise view of a position, primarily for listing purposes.
 */
public record PositionSummary(
    /**
      The unique identifier of the position.
     */
    Integer positionId,
    /**
     * The description/name of the position.
     */
    String description
) {
}
