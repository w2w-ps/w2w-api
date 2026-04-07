package com.w2w.api.scheduling.dto;

import java.util.List;

/**
 * Represents the overall response for a validation request, indicating whether conflicts were found
 * and providing a list of detected conflicts.
 *
 * @param hasConflicts A flag indicating if any conflicts were detected.
 * @param conflicts    A list of {@link ConflictItem} objects detailing the detected conflicts.
 */
public record ConflictResponse(boolean hasConflicts, List<ConflictItem> conflicts) {
}
