package com.w2w.api.manager.dto;

public record UpdateManagerRequest(
    String firstName,
    String lastName,
    String email,
    ManagerPermissionsDto permissions
) {}
