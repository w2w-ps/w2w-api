package com.w2w.api.login;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;

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
        assertTrue(response.isValid());
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
