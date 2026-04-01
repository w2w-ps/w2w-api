package com.w2w.api.scheduling.dto;

/**
 * Request DTO for conflict validation.
 * Contains the operation type and the shift data relevant to that operation.
 *
 * @param operationType The type of operation being validated (e.g., CREATE, UPDATE, REASSIGN).
 * @param shift     The {@link UpdateShiftRequest} containing the shift details or identifiers relevant to the operation.
 */
public record FindConflictRequest(OperationType operationType, UpdateShiftRequest shift) {
}
