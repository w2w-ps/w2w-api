package com.w2w.api.login;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
     * Fetches the user from USERS table by USUSERLOGINID, verifies the raw password
     * against the BCrypt-hashed USUSERLOGINPW, and returns a signed JWT on success or null on failure.
     */
    public String authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }

        Optional<User> userOpt = loginRepository.findByLoginId(username);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        if (passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtil.generateToken(username);
        }

        return null;
    }
}
