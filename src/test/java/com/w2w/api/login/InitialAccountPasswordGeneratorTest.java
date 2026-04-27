package com.w2w.api.login;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InitialAccountPasswordGeneratorTest {

    @Test
    void generate_returnsPasswordThatPassesComplexityValidation() {
        InitialAccountPasswordGenerator generator = new InitialAccountPasswordGenerator();
        LoginService loginService = new LoginService(null, null, null, null);

        PasswordValidationResponse response = loginService.validatePassword(generator.generate());

        assertTrue(response.isValid());
    }
}
