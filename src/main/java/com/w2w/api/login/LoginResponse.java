package com.w2w.api.login;

public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private String role;
    private String empType;
    private String displayName;
    private Integer userId;
    private Integer empId;

    public LoginResponse() {}

    public LoginResponse(boolean success, String message, String token, String role, String empType, String displayName, Integer userId, Integer empId) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.role = role;
        this.empType = empType;
        this.displayName = displayName;
        this.userId = userId;
        this.empId = empId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmpType() { return empType; }
    public void setEmpType(String empType) { this.empType = empType; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getEmpId() { return empId; }
    public void setEmpId(Integer empId) { this.empId = empId; }
}
