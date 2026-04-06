package com.w2w.api.login;

public record PasswordValidationRequest(String username, String oldPassword, String newPassword, String confirmPassword) {
}
