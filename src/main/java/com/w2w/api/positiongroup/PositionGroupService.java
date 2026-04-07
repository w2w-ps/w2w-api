package com.w2w.api.positiongroup;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.positiongroup.dto.PositionGroupSummary;
import com.w2w.api.positiongroup.dto.UpdatePositionGroupRequest;
import com.w2w.api.positiongroup.model.PositionGroup;
import com.w2w.api.positiongroup.repository.PositionGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PositionGroupService {

    private final PositionGroupRepository positionGroupRepository;
    private final PositionRepository positionRepository;

    public PositionGroupService(PositionGroupRepository positionGroupRepository, PositionRepository positionRepository) {
        this.positionGroupRepository = positionGroupRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional(readOnly = true)
    public List<PositionGroupSummary> getPositionGroups(String status) {
        return findPositionGroupsByStatus(TenantContext.getCurrentTenant(), status).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PositionGroupSummary> getPositionGroupById(Integer groupId) {
        return positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(
                        groupId,
                        TenantContext.getCurrentTenant()
                )
                .map(this::toSummary);
    }

    @Transactional
    public void createPositionGroup(String description, Collection<Integer> positionIds) {
        Integer resolvedCompanyId = CurrentTenant.requireCurrentTenant();
        PositionGroup positionGroup = new PositionGroup();
        positionGroup.setCompanyId(resolvedCompanyId);
        positionGroup.setDescription(description);
        positionGroup.setIsDeleted(false);
        positionGroup.setPositions(resolvePositions(List.copyOf(positionIds), resolvedCompanyId));
        positionGroupRepository.save(positionGroup);
    }

    @Transactional
    public void updatePositionGroup(Integer groupId, UpdatePositionGroupRequest request) {
        Integer resolvedCompanyId = CurrentTenant.requireCurrentTenant();
        PositionGroup positionGroup = requireActivePositionGroup(groupId, resolvedCompanyId);
        positionGroup.setDescription(request.description());
        positionGroup.setPositions(resolvePositions(request.positionIds(), resolvedCompanyId));
        positionGroupRepository.save(positionGroup);
    }

    @Transactional
    public void deletePositionGroup(Integer groupId) {
        PositionGroup positionGroup = requireActivePositionGroup(groupId, CurrentTenant.requireCurrentTenant());
        positionGroup.setIsDeleted(true);
        positionGroupRepository.save(positionGroup);
    }

    private PositionGroupSummary toSummary(PositionGroup positionGroup) {
        return new PositionGroupSummary(
                positionGroup.getGroupId(),
                positionGroup.getDescription(),
                positionGroup.getPositions().stream()
                        .map(this::toPositionSummary)
                        .toList()
        );
    }

    private PositionSummary toPositionSummary(Position position) {
        return new PositionSummary(position.getPositionId(), position.getDescription());
    }

    private PositionGroup requireActivePositionGroup(Integer groupId, Integer companyId) {
        return positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position group not found"));
    }

    private List<PositionGroup> findPositionGroupsByStatus(Integer companyId, String status) {
        return switch (status.toLowerCase()) {
            case "all" -> positionGroupRepository.findByCompanyId(companyId);
            case "active" -> positionGroupRepository.findByCompanyIdAndIsDeletedFalse(companyId);
            case "inactive" -> positionGroupRepository.findByCompanyIdAndIsDeletedTrue(companyId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter");
        };
    }

    private List<Position> resolvePositions(List<Integer> positionIds, Integer companyId) {
        if (positionIds == null || positionIds.isEmpty()) {
            return new ArrayList<>();
        }

        Set<Integer> uniquePositionIds = new HashSet<>(positionIds);
        List<Position> positions = positionRepository.findByPositionIdInAndCompanyId(uniquePositionIds, companyId);
        
        if (positions.size() != uniquePositionIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more position ids were invalid for the company");
        }

        Map<Integer, Position> positionMap = positions.stream()
                .collect(Collectors.toMap(Position::getPositionId, Function.identity()));

        return positionIds.stream()
                .map(positionMap::get)
                .toList();
    }
}
