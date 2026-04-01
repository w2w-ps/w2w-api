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
     * Authenticates the user and returns the User object if successful.
     */
    public java.util.Optional<User> authenticate(String username, String password) {
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
}
