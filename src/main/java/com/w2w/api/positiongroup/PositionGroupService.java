package com.w2w.api.positiongroup;

import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.positiongroup.dto.CreatePositionGroupRequest;
import com.w2w.api.positiongroup.dto.PositionGroupSummary;
import com.w2w.api.positiongroup.dto.UpdatePositionGroupRequest;
import com.w2w.api.positiongroup.model.PositionGroup;
import com.w2w.api.positiongroup.repository.PositionGroupRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

    public List<PositionGroupSummary> getPositionGroups(Integer companyId, String status) {
        List<PositionGroup> groups;
        if ("active".equalsIgnoreCase(status)) {
            groups = positionGroupRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        } else if ("inactive".equalsIgnoreCase(status)) {
            groups = positionGroupRepository.findByCompanyIdAndIsDeletedTrue(companyId);
        } else {
            groups = positionGroupRepository.findByCompanyId(companyId);
        }

        return groups.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    public Optional<PositionGroupSummary> getPositionGroupById(Integer groupId, Integer companyId) {
        return positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .map(this::mapToSummary);
    }

    public void createPositionGroup(CreatePositionGroupRequest request) {
        PositionGroup group = new PositionGroup();
        group.setCompanyId(request.companyId());
        group.setDescription(request.description());
        group.setIsDeleted(false);
        
        group.setPositions(resolvePositions(request.positionIds(), request.companyId()));
        
        positionGroupRepository.save(group);
    }

    public void updatePositionGroup(Integer groupId, Integer companyId, UpdatePositionGroupRequest request) {
        PositionGroup group = positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position group not found"));
        
        group.setDescription(request.description());
        group.setPositions(resolvePositions(request.positionIds(), companyId));
        
        positionGroupRepository.save(group);
    }

    public void deletePositionGroup(Integer groupId, Integer companyId) {
        PositionGroup group = positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position group not found"));
        
        group.setIsDeleted(true);
        positionGroupRepository.save(group);
    }

    private PositionGroupSummary mapToSummary(PositionGroup group) {
        List<PositionSummary> positions = group.getPositions().stream()
                .map(this::mapToPositionSummary)
                .toList();
        return new PositionGroupSummary(group.getGroupId(), group.getDescription(), positions);
    }

    private PositionSummary mapToPositionSummary(Position position) {
        return new PositionSummary(position.getPositionId(), position.getDescription());
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
