package com.w2w.api.login;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.w2w.api.employee.repository.EmployeeRepository;
import java.util.Optional;
import java.util.stream.Stream;

class PasswordValidationTest {

    private LoginService loginService;
    private LoginRepository loginRepository;
    private EmployeeRepository employeeRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        loginRepository = Mockito.mock(LoginRepository.class);
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        loginService = new LoginService(loginRepository, employeeRepository, jwtUtil, passwordEncoder);
    }

    @Test
    void testValidPassword() {
        PasswordValidationResponse response = loginService.validatePassword("Password123!");
        assertTrue(response.isValid());
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordCases")
    void validatePassword_rejectsInvalidPassword(String password, String expectedError) {
        PasswordValidationResponse response = loginService.validatePassword(password);

        assertFalse(response.isValid());
        assertTrue(response.errors().contains(expectedError));
    }

    private static Stream<Arguments> invalidPasswordCases() {
        return Stream.of(
                Arguments.of(null, "Password must be at least 8 characters long."),
                Arguments.of("password123!", "Password must contain at least one uppercase letter."),
                Arguments.of("PASSWORD123!", "Password must contain at least one lowercase letter."),
                Arguments.of("Password!", "Password must contain at least one number."),
                Arguments.of("Password123", "Password must contain at least one special character.")
        );
    }

    @Test
    void validatePassword_handlesLongInputWithoutRegexBacktracking() {
        PasswordValidationResponse response = loginService.validatePassword("a".repeat(10000) + "1!");

        assertFalse(response.isValid());
        assertTrue(response.errors().contains("Password must contain at least one uppercase letter."));
    }

    @Test
    void testUpdatePassword_Success() {
        User user = new User();
        user.setLoginId("testuser");
        user.setPassword("hashedOldPassword");

        Mockito.when(loginRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);
        Mockito.when(passwordEncoder.encode("Password123!")).thenReturn("hashedNewPassword");

        PasswordValidationResponse response = loginService.updatePassword("testuser", "oldPassword", "Password123!", "Password123!");
        
        assertTrue(response.isValid());
        assertEquals("Password updated successfully.", response.message());
        Mockito.verify(loginRepository).save(user);
    }

    @Test
    void testUpdatePassword_Mismatch() {
        PasswordValidationResponse response = loginService.updatePassword("testuser", "old", "New123!", "New123@");
        assertFalse(response.isValid());
        assertTrue(response.errors().contains("Passwords do not match."));
    }

    @Test
    void testUpdatePassword_Reuse() {
        PasswordValidationResponse response = loginService.updatePassword("testuser", "same", "same", "same");
        assertFalse(response.isValid());
        assertTrue(response.errors().contains("New password cannot be the same as the old password."));
    }

    @Test
    void testUpdatePassword_IncorrectOld() {
        User user = new User();
        user.setPassword("hashedOld");
        Mockito.when(loginRepository.findByLoginId("testuser")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.matches("wrong", "hashedOld")).thenReturn(false);

        PasswordValidationResponse response = loginService.updatePassword("testuser", "wrong", "New123!", "New123!");
        assertFalse(response.isValid());
        assertTrue(response.errors().contains("Incorrect old password."));
    }

    @Test
    void testResetUserAccount_Success() {
        User user = new User();
        user.setLoginId("oldUser");
        Mockito.when(loginRepository.findByLoginId("oldUser")).thenReturn(Optional.of(user));
        Mockito.when(passwordEncoder.encode("NewSecret123!")).thenReturn("hashedNewSecret");

        PasswordValidationResponse response = loginService.resetUserAccount("oldUser", "newUser", "NewSecret123!", "NewSecret123!");

        assertTrue(response.isValid());
        assertEquals("User account reset successfully.", response.message());
        assertEquals("newUser", user.getLoginId());
        assertEquals("hashedNewSecret", user.getPassword());
        Mockito.verify(loginRepository).save(user);
    }

    @Test
    void testResetUserAccount_WeakPassword() {
        User user = new User();
        Mockito.when(loginRepository.findByLoginId("any")).thenReturn(Optional.of(user));

        PasswordValidationResponse response = loginService.resetUserAccount("any", "any", "weak", "weak");

        assertFalse(response.isValid());
        assertTrue(response.errors().size() > 0);
    }
}
