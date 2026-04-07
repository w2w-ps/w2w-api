package com.w2w.api.manager.dto;

public record AddManagerRequest(
    String firstName,
    String lastName,
    String email,
    Integer companyId,
    Boolean emailInstructions,
    ManagerPermissionsDto permissions
) {}
