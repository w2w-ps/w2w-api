package com.w2w.api.timeoff.dto;

import jakarta.validation.constraints.NotNull;

public record ApproveTimeOffRequest(
        @NotNull(message = "Action is required") 
        Action action,
        
        String managerComments
) {
    public enum Action {
        APPROVE,
        DECLINE
    }
}
