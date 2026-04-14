package com.w2w.api.manager;

import com.w2w.api.login.User;
import com.w2w.api.manager.dto.AddManagerRequest;
import com.w2w.api.manager.dto.UpdateManagerRequest;
import com.w2w.api.manager.dto.ManagerResponse;

import org.springframework.http.HttpStatus;
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

    @GetMapping("/additional-managers")
    public ResponseEntity<List<ManagerResponse>> getAdditionalManagersByCompany() {
        return ResponseEntity.ok(managerService.getAdditionalManagersByCompany());
    }

    @PostMapping
    public ResponseEntity<Void> addManager(@RequestBody AddManagerRequest request) {
        managerService.addManager(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateManager(@PathVariable Integer id, @RequestBody UpdateManagerRequest request) {
        managerService.updateManager(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteManager(@PathVariable Integer id) {
        managerService.deleteManager(id);
        return ResponseEntity.noContent().build();
    }
}
