package com.w2w.api.login;

public class UserAccountResetRequest {
    private String currentUsername;
    private String newUsername;
    private String newPassword;
    private String confirmPassword;

    public UserAccountResetRequest() {}

    public UserAccountResetRequest(String currentUsername, String newUsername, String newPassword, String confirmPassword) {
        this.currentUsername = currentUsername;
        this.newUsername = newUsername;
        this.newPassword = newPassword;
        this.confirmPassword = confirmPassword;
    }

    public String getCurrentUsername() { return currentUsername; }
    public void setCurrentUsername(String currentUsername) { this.currentUsername = currentUsername; }

    public String getNewUsername() { return newUsername; }
    public void setNewUsername(String newUsername) { this.newUsername = newUsername; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
