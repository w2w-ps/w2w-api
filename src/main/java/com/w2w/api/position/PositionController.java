package com.w2w.api.position;

import com.w2w.api.config.TenantContext;
import com.w2w.api.position.dto.CreatePositionRequest;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.PositionsResponse;
import com.w2w.api.position.dto.UpdatePositionRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public ResponseEntity<PositionsResponse> getPositions(
            @RequestParam Integer companyId,
            @RequestParam(defaultValue = "all") String status
    ) {
        TenantContext.setCurrentTenant(companyId);
        List<PositionSummary> positions = positionService.getPositions(status);
        return ResponseEntity.ok(new PositionsResponse(positions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionSummary> getPositionById(@PathVariable("id") Integer positionId, @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        return positionService.getPositionById(positionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createPosition(@Valid @RequestBody CreatePositionRequest request) {
        TenantContext.setCurrentTenant(request.companyId());
        positionService.createPosition(request.description());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePosition(
            @PathVariable("id") Integer positionId,
            @RequestParam Integer companyId,
            @Valid @RequestBody UpdatePositionRequest request
    ) {
        TenantContext.setCurrentTenant(companyId);
        positionService.updatePosition(positionId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosition(@PathVariable("id") Integer positionId, @RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        positionService.deletePosition(positionId);
        return ResponseEntity.noContent().build();
    }
}
