package com.w2w.api.position.controller;

import com.w2w.api.config.TenantContext;
import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.position.dto.PositionGroupDto;
import com.w2w.api.position.dto.PositionsResponse;
import com.w2w.api.position.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class PositionController {
    @Autowired
    private PositionService positionService;

    @GetMapping
    public PositionsResponse getPositions(@RequestParam Integer companyId) {
        TenantContext.setCurrentTenant(companyId);
        List<PositionDto> positions = positionService.getPositionsByCompanyId(companyId);
        List<PositionGroupDto> groups = positionService.getPositionGroupsByCompanyId(companyId);
        return new PositionsResponse(positions, groups);
    }
}
