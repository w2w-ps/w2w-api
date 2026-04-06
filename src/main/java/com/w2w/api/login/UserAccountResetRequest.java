package com.w2w.api.login;

public record UserAccountResetRequest(String currentUsername, String newUsername, String newPassword, String confirmPassword) {
}
