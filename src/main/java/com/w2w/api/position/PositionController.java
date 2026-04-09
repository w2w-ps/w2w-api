package com.w2w.api.position;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.w2w.api.position.dto.CreatePositionRequest;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.PositionsResponse;
import com.w2w.api.position.dto.UpdatePositionRequest;

@RestController
@RequestMapping("/api/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping
    public ResponseEntity<PositionsResponse> getPositions(
            @RequestParam(defaultValue = "active") String status
    ) {
        List<PositionSummary> positions = positionService.get(status);
        return ResponseEntity.ok(new PositionsResponse(positions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionSummary> getPositionById(@PathVariable("id") Integer positionId) {
        return positionService.getPositionSummaryById(positionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createPosition(@Valid @RequestBody CreatePositionRequest request) {
        positionService.create(request.description());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePosition(
            @PathVariable("id") Integer positionId,
            @Valid @RequestBody UpdatePositionRequest request
    ) {
        positionService.update(positionId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePosition(@PathVariable("id") Integer positionId) {
        positionService.delete(positionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restorePosition(@PathVariable("id") Integer positionId) {
        positionService.restore(positionId);
        return ResponseEntity.noContent().build();
    }
}
