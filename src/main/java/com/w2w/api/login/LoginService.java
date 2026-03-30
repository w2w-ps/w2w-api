package com.w2w.api.login;

import org.springframework.stereotype.Service;

@Service
public class LoginService {

    private final LoginRepository loginRepository;

    public LoginService(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    public boolean authenticate(String username, String password) {
        if (username == null || password == null) {
            return false;
        }
        String storedPassword = loginRepository.getPassword(username);
        return password.equals(storedPassword);
    }
}
