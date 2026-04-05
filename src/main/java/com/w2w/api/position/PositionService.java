package com.w2w.api.position;

import com.w2w.api.position.dto.CreatePositionRequest;
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
            position.getSkillId(),
            position.getDescription()
        );
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getPositions(Integer companyId, String status) {
        return findPositionsByStatus(companyId, status).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getAllPositions(Integer companyId) {
        return getPositions(companyId, "all");
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getActivePositions(Integer companyId) {
        return positionRepository.findByCompanyIdAndIsDeletedFalse(companyId).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PositionSummary> getNonActivePositions(Integer companyId) {
        return positionRepository.findByCompanyIdAndIsDeletedTrue(companyId).stream()
                .map(this::convertToPositionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<PositionSummary> getPositionById(Integer skillId, Integer companyId) {
        return positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId)
                .map(this::convertToPositionSummary);
    }

    @Transactional
    public void createPosition(CreatePositionRequest request) {
        Position position = new Position();
        position.setCompanyId(request.companyId());
        position.setDescription(request.description());
        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.now());

        positionRepository.save(position);
    }

    @Transactional
    public void updatePosition(Integer skillId, Integer companyId, UpdatePositionRequest request) {
        Position position = requireActivePosition(skillId, companyId);

        position.setDescription(request.description());
        position.setTimestamp(LocalDateTime.now());

        positionRepository.save(position);
    }

    @Transactional
    public void deletePosition(Integer skillId, Integer companyId) {
        Position position = requireActivePosition(skillId, companyId);

        position.setIsDeleted(true);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    private Position requireActivePosition(Integer skillId, Integer companyId) {
        return positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId)
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
