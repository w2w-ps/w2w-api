package com.w2w.api.login;

public record LoginResponse(boolean success, String message, String token, String role, String empType, String displayName, Integer userId, Integer empId, Integer companyId) {
}
