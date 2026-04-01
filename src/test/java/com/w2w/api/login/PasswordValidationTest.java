package com.w2w.api.login;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordValidationTest {

    private LoginService loginService;
    private LoginRepository loginRepository;
    private JwtUtil jwtUtil;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        loginRepository = Mockito.mock(LoginRepository.class);
        jwtUtil = Mockito.mock(JwtUtil.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        loginService = new LoginService(loginRepository, jwtUtil, passwordEncoder);
    }

    @Test
    void testValidPassword() {
        PasswordValidationResponse response = loginService.validatePassword("Password123!");
        assertTrue(response.getIsValid());
    }

    @Test
    void testUpdatePassword_Success() {
        User user = new User();
        user.setLoginId("testuser");
        user.setPassword("hashedOldPassword");

        Mockito.when(loginRepository.findByLoginId("testuser")).thenReturn(java.util.Optional.of(user));
        Mockito.when(passwordEncoder.matches("oldPassword", "hashedOldPassword")).thenReturn(true);
        Mockito.when(passwordEncoder.encode("Password123!")).thenReturn("hashedNewPassword");

        PasswordValidationResponse response = loginService.updatePassword("testuser", "oldPassword", "Password123!", "Password123!");
        
        assertTrue(response.getIsValid());
        assertEquals("Password updated successfully.", response.getMessage());
        Mockito.verify(loginRepository).save(user);
    }

    @Test
    void testUpdatePassword_Mismatch() {
        PasswordValidationResponse response = loginService.updatePassword("testuser", "old", "New123!", "New123@");
        assertFalse(response.getIsValid());
        assertTrue(response.getErrors().contains("Passwords do not match."));
    }

    @Test
    void testUpdatePassword_Reuse() {
        PasswordValidationResponse response = loginService.updatePassword("testuser", "same", "same", "same");
        assertFalse(response.getIsValid());
        assertTrue(response.getErrors().contains("New password cannot be the same as the old password."));
    }

    @Test
    void testUpdatePassword_IncorrectOld() {
        User user = new User();
        user.setPassword("hashedOld");
        Mockito.when(loginRepository.findByLoginId("testuser")).thenReturn(java.util.Optional.of(user));
        Mockito.when(passwordEncoder.matches("wrong", "hashedOld")).thenReturn(false);

        PasswordValidationResponse response = loginService.updatePassword("testuser", "wrong", "New123!", "New123!");
        assertFalse(response.getIsValid());
        assertTrue(response.getErrors().contains("Incorrect old password."));
    }
}
