package com.w2w.api.login;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = loginService.authenticate(request.getUsername(), request.getPassword());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String roleName = (user.getRole() != null) ? user.getRole().getName() : "ROLE_USER";
            String token = loginService.generateToken(user.getLoginId(), roleName);
            
            String empTypeName = (user.getEmpType() != null) ? user.getEmpType().getName() : null;
            String displayName = (user.getEmployee() != null) ? 
                user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName() : user.getLoginId();

            return ResponseEntity.ok(new LoginResponse(true, "Login successful", token, roleName, empTypeName, displayName, user.getId(), user.getEmployeeId()));
        } else {
            return ResponseEntity.status(401).body(new LoginResponse(false, "Invalid credentials", null, null, null, null, null, null));
        }
    }


    @PostMapping("/login/update-password")
    public ResponseEntity<PasswordValidationResponse> updatePassword(@RequestBody PasswordValidationRequest request) {
        return ResponseEntity.ok(loginService.updatePassword(
            request.getUsername(), 
            request.getOldPassword(), 
            request.getNewPassword(), 
            request.getConfirmPassword()
        ));
    }

    @PostMapping("/login/reset-user-account")
    public ResponseEntity<PasswordValidationResponse> resetUserAccount(@RequestBody UserAccountResetRequest request) {
        return ResponseEntity.ok(loginService.resetUserAccount(
            request.getCurrentUsername(),
            request.getNewUsername(),
            request.getNewPassword(),
            request.getConfirmPassword()
        ));
    }
}