package com.w2w.api.login;

import com.w2w.api.employee.model.Employee;
import com.w2w.api.employee.repository.EmployeeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoginService {
    private static final String USER_NOT_FOUND_MESSAGE = "User not found.";
    private static final String SPECIAL_PASSWORD_CHARACTERS = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";

    private final LoginRepository loginRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public LoginService(LoginRepository loginRepository, EmployeeRepository employeeRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.loginRepository = loginRepository;
        this.employeeRepository = employeeRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates the user and returns the User object if successful.
     */
    @Transactional
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
            if (user.getEmployee() != null) {
                Employee employee = user.getEmployee();
                employee.setLastLogon(LocalDateTime.now());
                Integer currentCount = employee.getLogonCount();
                employee.setLogonCount(currentCount == null ? 1 : currentCount + 1);
                employeeRepository.save(employee);
            }
            return Optional.of(user);
        }
    
        return Optional.empty();
    }

    public String generateToken(String username, String role) {
        return jwtUtil.generateToken(username, role);
    }

    public PasswordValidationResponse validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        boolean isValid = true;

        if (password == null || password.length() < 8) {
            errors.add("Password must be at least 8 characters long.");
            isValid = false;
        }

        if (password != null) {
            PasswordCharacterSummary characterSummary = summarizePasswordCharacters(password);

            if (!characterSummary.hasUppercase()) {
                errors.add("Password must contain at least one uppercase letter.");
                isValid = false;
            }
            if (!characterSummary.hasLowercase()) {
                errors.add("Password must contain at least one lowercase letter.");
                isValid = false;
            }
            if (!characterSummary.hasDigit()) {
                errors.add("Password must contain at least one number.");
                isValid = false;
            }
            if (!characterSummary.hasSpecial()) {
                errors.add("Password must contain at least one special character.");
                isValid = false;
            }
        }

        return new PasswordValidationResponse(isValid, errors, null);
    }

    private PasswordCharacterSummary summarizePasswordCharacters(String password) {
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (int i = 0; i < password.length(); i++) {
            char current = password.charAt(i);
            hasUppercase = hasUppercase || Character.isUpperCase(current);
            hasLowercase = hasLowercase || Character.isLowerCase(current);
            hasDigit = hasDigit || Character.isDigit(current);
            hasSpecial = hasSpecial || SPECIAL_PASSWORD_CHARACTERS.indexOf(current) >= 0;
        }

        return new PasswordCharacterSummary(hasUppercase, hasLowercase, hasDigit, hasSpecial);
    }

    private record PasswordCharacterSummary(boolean hasUppercase, boolean hasLowercase, boolean hasDigit, boolean hasSpecial) {
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
            errors.add(USER_NOT_FOUND_MESSAGE);
            return new PasswordValidationResponse(false, errors, USER_NOT_FOUND_MESSAGE);
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            errors.add("Incorrect old password.");
            return new PasswordValidationResponse(false, errors, "Authentication failed.");
        }

        // 3. Complexity validation
        PasswordValidationResponse valResponse = validatePassword(newPassword);
        if (!valResponse.isValid()) {
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
            errors.add(USER_NOT_FOUND_MESSAGE);
            return new PasswordValidationResponse(false, errors, USER_NOT_FOUND_MESSAGE);
        }

        User user = userOpt.get();

        // Complexity validation
        PasswordValidationResponse valResponse = validatePassword(newPassword);
        if (!valResponse.isValid()) {
            return valResponse;
        }

        // Persistence
        user.setLoginId(newUsername);
        user.setPassword(passwordEncoder.encode(newPassword));
        loginRepository.save(user);

        return new PasswordValidationResponse(true, new ArrayList<>(), "User account reset successfully.");
    }
}
