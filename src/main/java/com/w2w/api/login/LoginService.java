package com.w2w.api.login;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final LoginRepository loginRepository;
    private final JwtUtil jwtUtil;

    public LoginService(LoginRepository loginRepository, JwtUtil jwtUtil) {
        this.loginRepository = loginRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates the user and returns a JWT token on success, or null on failure.
     */
    public String authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        String storedPassword = loginRepository.getPassword(username);
        if (password.equals(storedPassword)) {
            return jwtUtil.generateToken(username);
        }
        return null;
    }
}
