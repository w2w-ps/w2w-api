package com.w2w.api.login;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoginService {

    private final LoginRepository loginRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginService(LoginRepository loginRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates the user and returns the User object if successful.
     */
    public Optional<User> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = loginRepository.findByLoginId(username);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (passwordEncoder.matches(password, user.getPassword())) {
            return Optional.of(user);
        }
    
        return Optional.empty();
    }

    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }

    public PasswordValidationResponse validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        boolean isValid = true;

        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters long.");
            isValid = false;
        }

        if (password != null) {
            if (!password.matches(".*[A-Z].*")) {
                errors.add("Password must contain at least one uppercase letter.");
                isValid = false;
            }
            if (!password.matches(".*[a-z].*")) {
                errors.add("Password must contain at least one lowercase letter.");
                isValid = false;
            }
            if (!password.matches(".*[0-9].*")) {
                errors.add("Password must contain at least one number.");
                isValid = false;
            }
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
                errors.add("Password must contain at least one special character.");
                isValid = false;
            }
        }

        return new PasswordValidationResponse(isValid, errors);
    }

    public PasswordValidationResponse updatePassword(String username, String oldPassword, String newPassword, String confirmPassword) {
        List<String> errors = new ArrayList<>();

        // 1. Basic matching and identified checks
        if (username == null || username.isEmpty()) {
            errors.add("Username is required.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            errors.add("Passwords do not match.");
        }
        if (newPassword != null && newPassword.equals(oldPassword)) {
            errors.add("New password cannot be the same as the old password.");
        }

        if (!errors.isEmpty()) {
            return new PasswordValidationResponse(false, errors, "Validation failed.");
        }

        // 2. User existence or authentication check
        Optional<User> userOpt = loginRepository.findByLoginId(username);
        if (userOpt.isEmpty()) {
            errors.add("User not found.");
            return new PasswordValidationResponse(false, errors, "User not found.");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            errors.add("Incorrect old password.");
            return new PasswordValidationResponse(false, errors, "Authentication failed.");
        }

        // 3. Complexity validation
        PasswordValidationResponse valResponse = validatePassword(newPassword);
        if (!valResponse.getIsValid()) {
            return valResponse;
        }

        // 4. Persistence
        user.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(user);

        return new PasswordValidationResponse(true, new ArrayList<>(), "Password updated successfully.");
    }

    public PasswordValidationResponse resetUserAccount(String currentUsername, String newUsername, String newPassword, String confirmPassword) {
        List<String> errors = new ArrayList<>();

        if (currentUsername == null || currentUsername.isEmpty()) {
            errors.add("Current username is required.");
        }
        if (newUsername == null || newUsername.isEmpty()) {
            errors.add("New username is required.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            errors.add("Passwords do not match.");
        }

        if (!errors.isEmpty()) {
            return new PasswordValidationResponse(false, errors, "Validation failed.");
        }

        // Locate user
        Optional<User> userOpt = loginRepository.findByLoginId(currentUsername);
        if (userOpt.isEmpty()) {
            errors.add("User not found.");
            return new PasswordValidationResponse(false, errors, "User not found.");
        }

        User user = userOpt.get();

        // Complexity validation
        PasswordValidationResponse valResponse = validatePassword(newPassword);
        if (!valResponse.getIsValid()) {
            return valResponse;
        }

        // Persistence
        user.setLoginId(newUsername);
        user.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(user);

        return new PasswordValidationResponse(true, new ArrayList<>(), "User account reset successfully.");
    }
}
