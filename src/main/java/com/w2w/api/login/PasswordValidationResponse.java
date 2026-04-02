package com.w2w.api.login;

import java.util.List;

public record PasswordValidationResponse(boolean isValid, List<String> errors, String message) {
}
