package com.w2w.api.position;

import com.w2w.api.position.dto.CreatePositionRequest;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public List<PositionSummary> getPositions(Integer companyId, String status) {
        List<Position> positions;
        if ("active".equalsIgnoreCase(status)) {
            positions = positionRepository.findByCompanyIdAndIsDeletedFalse(companyId);
        } else if ("inactive".equalsIgnoreCase(status)) {
            positions = positionRepository.findByCompanyIdAndIsDeletedTrue(companyId);
        } else {
            positions = positionRepository.findByCompanyId(companyId);
        }

        return positions.stream()
                .map(position -> new PositionSummary(
                        position.getPositionId(),
                        position.getDescription()
                ))
                .collect(Collectors.toList());
    }

    public void createPosition(CreatePositionRequest request) {
        Position position = new Position();
        position.setCompanyId(request.companyId());
        position.setDescription(request.description());
        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    public Optional<PositionSummary> getPositionById(Integer positionId, Integer companyId) {
        return positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId)
                .map(position -> new PositionSummary(position.getPositionId(), position.getDescription()));
    }

    public void updatePosition(Integer positionId, Integer companyId, UpdatePositionRequest request) {
        Position position = requireActivePosition(positionId, companyId);
        position.setDescription(request.description());
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    public void deletePosition(Integer positionId, Integer companyId) {
        Position position = requireActivePosition(positionId, companyId);
        position.setIsDeleted(true);
        position.setTimestamp(LocalDateTime.now());
        positionRepository.save(position);
    }

    private Position requireActivePosition(Integer positionId, Integer companyId) {
        return positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));
    }
}
