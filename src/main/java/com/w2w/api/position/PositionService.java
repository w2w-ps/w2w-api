package com.w2w.api.position;

import com.w2w.api.config.TenantContext;
import com.w2w.api.config.exception.ResourceNotFoundException;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    private PositionSummary convertToPositionSummary(Position position) {
        return new PositionSummary(
                position.getPositionId(),
                position.getDescription()
        );
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> get(String status) {
        return findPositionsByStatus(TenantContext.getCurrentTenant(), status).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PositionSummary> getPositionSummaryById(Integer positionId) {
        return getById(positionId)
                .map(this::convertToPositionSummary);
    }

    private Optional<Position> getById(Integer positionId) {
        return positionRepository.findByPositionIdAndCompanyId(
                        positionId,
                        TenantContext.getCurrentTenant()
                );
    }

    @Transactional
    @PreAuthorize("@positionPolicy.canManage(authentication)")
    public void create(String description) {
        Integer companyId = TenantContext.getCurrentTenant();

        Position position = new Position();
        position.setCompanyId(companyId);
        position.setDescription(description);
        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    @Transactional
    @PreAuthorize("@positionPolicy.canManage(authentication)")
    public void update(Integer positionId, UpdatePositionRequest request) {
        Integer companyId = TenantContext.getCurrentTenant();

        Position position = requireActivePosition(positionId, companyId);
        position.setDescription(request.description());
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    @Transactional
    @PreAuthorize("@positionPolicy.canManage(authentication)")
    public void delete(Integer positionId) {
        Integer companyId = TenantContext.getCurrentTenant();
        Position position = requirePosition(positionId, companyId);
        if (Boolean.TRUE.equals(position.getIsDeleted())) {
            return;
        }

        position.setIsDeleted(true);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    @Transactional
    @PreAuthorize("@positionPolicy.canManage(authentication)")
    public void restore(Integer positionId) {
        Integer companyId = TenantContext.getCurrentTenant();
        Position position = requirePosition(positionId, companyId);
        if (!Boolean.TRUE.equals(position.getIsDeleted())) {
            return;
        }

        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    private Position requireActivePosition(Integer positionId, Integer companyId) {
        Position position = requirePosition(positionId, companyId);
        if (Boolean.TRUE.equals(position.getIsDeleted())) {
            throw new ResourceNotFoundException("Position not found");
        }
        return position;
    }

    private Position requirePosition(Integer positionId, Integer companyId) {
        return positionRepository.findByPositionIdAndCompanyId(positionId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
    }

    private List<Position> findPositionsByStatus(Integer companyId, String status) {
        return switch (status.toLowerCase()) {
            case "all" -> positionRepository.findByCompanyId(companyId);
            case "active" -> positionRepository.findByCompanyIdAndIsDeletedFalse(companyId);
            case "inactive" -> positionRepository.findByCompanyIdAndIsDeletedTrue(companyId);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid status. Allowed values: all, active, inactive."
            );
        };
    }
}
