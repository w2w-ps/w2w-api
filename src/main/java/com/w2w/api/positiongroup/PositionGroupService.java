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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<PositionGroupSummary> getPositionGroups(Integer companyId, String status) {
        return findPositionGroupsByStatus(companyId, status).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PositionGroupSummary> getPositionGroupById(Integer groupId, Integer companyId) {
        return positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(groupId, companyId)
                .map(this::toSummary);
    }

    @Transactional
    public void createPositionGroup(CreatePositionGroupRequest request) {
        PositionGroup positionGroup = new PositionGroup();
        positionGroup.setCompanyId(request.companyId());
        positionGroup.setDescription(request.description());
        positionGroup.setIsDeleted(false);
        positionGroup.setPositions(resolvePositions(request.positionIds(), request.companyId()));
        positionGroupRepository.save(positionGroup);
    }

    @Transactional
    public void updatePositionGroup(Integer groupId, Integer companyId, UpdatePositionGroupRequest request) {
        PositionGroup positionGroup = requireActivePositionGroup(groupId, companyId);
        positionGroup.setDescription(request.description());
        positionGroup.setPositions(resolvePositions(request.positionIds(), companyId));
        positionGroupRepository.save(positionGroup);
    }

    @Transactional
    public void deletePositionGroup(Integer groupId, Integer companyId) {
        PositionGroup positionGroup = requireActivePositionGroup(groupId, companyId);
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
        return new PositionSummary(position.getSkillId(), position.getDescription());
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

    private List<Position> resolvePositions(Collection<Integer> requestedPositionIds, Integer companyId) {
        LinkedHashSet<Integer> uniquePositionIds = new LinkedHashSet<>(requestedPositionIds);
        if (uniquePositionIds.isEmpty()) {
            return List.of();
        }

        List<Position> positions = positionRepository.findBySkillIdInAndCompanyId(uniquePositionIds, companyId);
        if (positions.size() != uniquePositionIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more positions were not found for the company"
            );
        }

        Map<Integer, Position> positionsById = positions.stream()
                .collect(Collectors.toMap(Position::getSkillId, Function.identity()));

        return uniquePositionIds.stream()
                .map(positionId -> positionsById.get(positionId))
                .toList();
    }
}
