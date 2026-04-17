package com.w2w.api.positiongroup;

import com.w2w.api.positiongroup.dto.CreatePositionGroupRequest;
import com.w2w.api.positiongroup.dto.PositionGroupSummary;
import com.w2w.api.positiongroup.dto.PositionGroupsResponse;
import com.w2w.api.positiongroup.dto.UpdatePositionGroupRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/position-groups")
public class PositionGroupController {

    private final PositionGroupService positionGroupService;

    public PositionGroupController(PositionGroupService positionGroupService) {
        this.positionGroupService = positionGroupService;
    }

    @GetMapping
    public ResponseEntity<PositionGroupsResponse> getPositionGroups(
            @RequestParam(defaultValue = "all") String status
    ) {
        return ResponseEntity.ok(new PositionGroupsResponse(positionGroupService.getPositionGroups(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PositionGroupSummary> getPositionGroupById(@PathVariable("id") Integer groupId) {
        return positionGroupService.getPositionGroupById(groupId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createPositionGroup(@Valid @RequestBody CreatePositionGroupRequest request) {
        positionGroupService.createPositionGroup(request.description(), request.positionIds());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePositionGroup(
            @PathVariable("id") Integer groupId,
            @Valid @RequestBody UpdatePositionGroupRequest request
    ) {
        positionGroupService.updatePositionGroup(groupId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePositionGroup(@PathVariable("id") Integer groupId) {
        positionGroupService.deletePositionGroup(groupId);
        return ResponseEntity.noContent().build();
    }
}
