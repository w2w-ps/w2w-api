package com.w2w.api.manager;

import com.w2w.api.login.User;
import com.w2w.api.manager.dto.AddManagerRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @PostMapping
    public ResponseEntity<User> addManager(@RequestBody AddManagerRequest request) {
        User manager = managerService.addManager(request);
        return ResponseEntity.ok(manager);
    }
}
