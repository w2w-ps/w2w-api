package com.w2w.api.manager;

import com.w2w.api.login.User;
import com.w2w.api.manager.dto.AddManagerRequest;
import com.w2w.api.manager.dto.UpdateManagerRequest;
import com.w2w.api.manager.dto.ManagerResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping("/add-managers/company/{companyId}")
    public ResponseEntity<List<ManagerResponse>> listAddManagers(@PathVariable Integer companyId) {
        return ResponseEntity.ok(managerService.getAddManagersByCompany(companyId));
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
