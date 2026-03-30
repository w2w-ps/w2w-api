package com.w2w.api.login;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class LoginRepository {
    private final Map<String, String> users = new HashMap<>();

    public LoginRepository() {
        users.put("admin", "admin123");
        users.put("user", "user123");
    }

    public String getPassword(String username) {
        return users.get(username);
    }
}
