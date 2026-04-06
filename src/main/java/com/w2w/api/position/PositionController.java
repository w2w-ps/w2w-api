package com.w2w.api.position;

import com.w2w.api.config.TenantContext;
import com.w2w.api.position.dto.PositionGroupSummary;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.PositionsResponse;
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
        List<PositionSummary> positions = positionService.getPositionsByCompanyId(companyId);
        List<PositionGroupSummary> groups = positionService.getPositionGroupsByCompanyId(companyId);
        return new PositionsResponse(positions, groups);
    }
}
