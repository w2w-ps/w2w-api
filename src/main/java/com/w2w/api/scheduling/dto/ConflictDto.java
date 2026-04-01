package com.w2w.api.scheduling.dto;

/**
 * Represents a single conflict detected during validation.
 *
 * @param field   The name of the field or property where the conflict occurred.
 * @param message A human-readable description of the conflict.
 */
public record ConflictDto(String field, String message) {
}
