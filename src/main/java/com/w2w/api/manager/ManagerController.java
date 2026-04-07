package com.w2w.api.manager;

import com.w2w.api.login.User;
import com.w2w.api.manager.dto.AddManagerRequest;
import com.w2w.api.manager.dto.UpdateManagerRequest;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PutMapping("/{id}")
    public ResponseEntity<User> updateManager(@PathVariable Integer id, @RequestBody UpdateManagerRequest request) {
        User manager = managerService.updateManager(id, request);
        return ResponseEntity.ok(manager);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteManager(@PathVariable Integer id) {
        managerService.deleteManager(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Manager deleted successfully");
        return ResponseEntity.ok(response);
    }
}
