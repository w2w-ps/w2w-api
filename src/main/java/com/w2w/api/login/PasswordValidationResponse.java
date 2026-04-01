package com.w2w.api.login;

import java.util.List;

public class PasswordValidationResponse {
    private boolean isValid;
    private List<String> errors;
    private String message;

    public PasswordValidationResponse() {}

    public PasswordValidationResponse(boolean isValid, List<String> errors) {
        this.isValid = isValid;
        this.errors = errors;
    }

    public PasswordValidationResponse(boolean isValid, List<String> errors, String message) {
        this.isValid = isValid;
        this.errors = errors;
        this.message = message;
    }

    public boolean getIsValid() { return isValid; }
    public void setIsValid(boolean isValid) { this.isValid = isValid; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
