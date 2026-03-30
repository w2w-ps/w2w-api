package com.w2w.api.login;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        boolean isValid = loginService.authenticate(request.getUsername(), request.getPassword());

        if (isValid) {
            return ResponseEntity.ok(new LoginResponse(true, "Login successful"));
        } else {
            return ResponseEntity.status(401).body(new LoginResponse(false, "Invalid credentials"));
        }
    }
}