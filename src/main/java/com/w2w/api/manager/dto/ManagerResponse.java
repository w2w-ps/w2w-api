package com.w2w.api.manager.dto;

import java.time.LocalDateTime;

public record ManagerResponse(
    Integer userId,
    String firstName,
    String lastName,
    String email,
    LocalDateTime lastSignIn,
    String roleName,
    ManagerPermissionsDto permissions
) {}
