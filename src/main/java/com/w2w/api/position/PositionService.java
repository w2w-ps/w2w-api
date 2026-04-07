package com.w2w.api.position;

import com.w2w.api.config.CurrentTenant;
import com.w2w.api.config.TenantContext;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.springframework.http.HttpStatus;
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
    public List<PositionSummary> getPositions(String status) {
        return findPositionsByStatus(TenantContext.getCurrentTenant(), status).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getAllPositions() {
        return getPositions("all");
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getActivePositions() {
        return positionRepository.findByCompanyIdAndIsDeletedFalse(TenantContext.getCurrentTenant()).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getNonActivePositions() {
        return positionRepository.findByCompanyIdAndIsDeletedTrue(TenantContext.getCurrentTenant()).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PositionSummary> getPositionById(Integer positionId) {
        return positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(
                        positionId,
                        TenantContext.getCurrentTenant()
                )
                .map(this::convertToPositionSummary);
    }

    @Transactional
    public void createPosition(String description) {
        Position position = new Position();
        position.setCompanyId(CurrentTenant.requireCurrentTenant());
        position.setDescription(description);
        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    @Transactional
    public void updatePosition(Integer positionId, UpdatePositionRequest request) {
        Position position = requireActivePosition(positionId, CurrentTenant.requireCurrentTenant());
        position.setDescription(request.description());
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    @Transactional
    public void deletePosition(Integer positionId) {
        Position position = requireActivePosition(positionId, CurrentTenant.requireCurrentTenant());
        position.setIsDeleted(true);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    private Position requireActivePosition(Integer positionId, Integer companyId) {
        return positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));
    }

    private List<Position> findPositionsByStatus(Integer companyId, String status) {
        return switch (status.toLowerCase()) {
            case "all" -> positionRepository.findByCompanyId(companyId);
            case "active" -> positionRepository.findByCompanyIdAndIsDeletedFalse(companyId);
            case "inactive" -> positionRepository.findByCompanyIdAndIsDeletedTrue(companyId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status filter");
        };
    }
}
