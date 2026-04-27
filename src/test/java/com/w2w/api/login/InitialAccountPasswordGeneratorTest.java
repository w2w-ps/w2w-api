package com.w2w.api.login;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialAccountPasswordGeneratorTest {

    @Test
    void generate_returnsPasswordThatPassesComplexityValidation() {
        InitialAccountPasswordGenerator generator = new InitialAccountPasswordGenerator();
        LoginService loginService = new LoginService(null, null, null, null);
        String password = generator.generate();

        PasswordValidationResponse response = loginService.validatePassword(password);

        assertEquals(12, password.length());
        assertTrue(response.isValid());
    }
}
