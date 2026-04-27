package com.w2w.api.login;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class InitialAccountPasswordGenerator {
    private static final int RANDOM_CHARACTER_COUNT = 28;
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{};':\"\\|,.<>/?";
    private static final String ALL_ALLOWED = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder(RANDOM_CHARACTER_COUNT + 4);
        password.append(randomFrom(UPPERCASE));
        password.append(randomFrom(LOWERCASE));
        password.append(randomFrom(DIGITS));
        password.append(randomFrom(SPECIAL));

        for (int i = 0; i < RANDOM_CHARACTER_COUNT; i++) {
            password.append(randomFrom(ALL_ALLOWED));
        }

        shuffle(password);
        return password.toString();
    }

    private char randomFrom(String characters) {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }

    private void shuffle(StringBuilder value) {
        for (int i = value.length() - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char current = value.charAt(i);
            value.setCharAt(i, value.charAt(j));
            value.setCharAt(j, current);
        }
    }
}
