package com.w2w.api.position;

import com.w2w.api.position.dto.CreatePositionRequest;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    private Integer companyId;
    private Integer skillId;
    private Position position;

    @BeforeEach
    void setUp() {
        companyId = 1;
        skillId = 101;
        position = new Position();
        position.setSkillId(skillId);
        position.setCompanyId(companyId);
        position.setDescription("Test Position");
        position.setIsDeleted(false);
        position.setTimestamp(LocalDateTime.now());
    }

    @Test
    void getPositions_shouldReturnListOfPositions() {
        Position deletedPosition = new Position();
        deletedPosition.setSkillId(102);
        deletedPosition.setCompanyId(companyId);
        deletedPosition.setDescription("Deleted Position");
        deletedPosition.setIsDeleted(true);
        deletedPosition.setTimestamp(LocalDateTime.now());

        when(positionRepository.findByCompanyId(companyId))
                .thenReturn(Arrays.asList(position, deletedPosition));

        List<PositionSummary> result = positionService.getPositions(companyId, "all"); // Changed List type

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(p -> p.positionId().equals(skillId)));
        assertTrue(result.stream().anyMatch(p -> p.positionId().equals(102)));
        verify(positionRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    void getPositions_shouldReturnListOfActivePositions() {
        when(positionRepository.findByCompanyIdAndIsDeletedFalse(companyId))
                .thenReturn(Arrays.asList(position));

        List<PositionSummary> result = positionService.getPositions(companyId, "active"); // Changed List type

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(skillId, result.get(0).positionId()); // Changed from skillId
        verify(positionRepository, times(1)).findByCompanyIdAndIsDeletedFalse(companyId);
    }

    @Test
    void getPositions_shouldReturnListOfInactivePositions() {
        Position deletedPosition = new Position();
        deletedPosition.setSkillId(skillId);
        deletedPosition.setCompanyId(companyId);
        deletedPosition.setDescription("Deleted Position");
        deletedPosition.setIsDeleted(true);
        deletedPosition.setTimestamp(LocalDateTime.now());

        when(positionRepository.findByCompanyIdAndIsDeletedTrue(companyId))
                .thenReturn(Arrays.asList(deletedPosition));

        List<PositionSummary> result = positionService.getPositions(companyId, "inactive"); // Changed List type

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals(skillId, result.get(0).positionId());
        assertEquals("Deleted Position", result.get(0).description());
        verify(positionRepository, times(1)).findByCompanyIdAndIsDeletedTrue(companyId);
    }

    @Test
    void getPositions_shouldThrowExceptionWhenStatusUnsupported() {
        assertThrows(ResponseStatusException.class, () -> positionService.getPositions(companyId, "archived"));
        verify(positionRepository, never()).findByCompanyId(companyId);
        verify(positionRepository, never()).findByCompanyIdAndIsDeletedFalse(companyId);
        verify(positionRepository, never()).findByCompanyIdAndIsDeletedTrue(companyId);
    }

    @Test
    void getPositionById_shouldReturnPositionWhenFound() {
        when(positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId))
                .thenReturn(Optional.of(position));

        Optional<PositionSummary> result = positionService.getPositionById(skillId, companyId); // Changed Optional type

        assertTrue(result.isPresent());
        assertEquals(skillId, result.get().positionId()); // Changed from skillId
        verify(positionRepository, times(1)).findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId);
    }

    @Test
    void getPositionById_shouldReturnEmptyWhenNotFound() {
        when(positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId))
                .thenReturn(Optional.empty());

        Optional<PositionSummary> result = positionService.getPositionById(skillId, companyId); // Changed Optional type

        assertFalse(result.isPresent());
        verify(positionRepository, times(1)).findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId);
    }

    @Test
    void createPosition_shouldSavePosition() {
        CreatePositionRequest request = new CreatePositionRequest(companyId, "New Position");
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        positionService.createPosition(request);

        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void updatePosition_shouldUpdateExistingPosition() {
        UpdatePositionRequest request = new UpdatePositionRequest("Updated Description");
        when(positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        positionService.updatePosition(skillId, companyId, request);

        assertEquals("Updated Description", position.getDescription());
        verify(positionRepository, times(1)).findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId);
        verify(positionRepository, times(1)).save(position);
    }

    @Test
    void updatePosition_shouldThrowExceptionWhenNotFound() {
        UpdatePositionRequest request = new UpdatePositionRequest("Updated Description");
        when(positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> positionService.updatePosition(skillId, companyId, request));
        verify(positionRepository, times(1)).findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId);
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void deletePosition_shouldSoftDeletePosition() {
        when(positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId))
                .thenReturn(Optional.of(position));
        when(positionRepository.save(any(Position.class))).thenReturn(position);

        positionService.deletePosition(skillId, companyId);

        assertTrue(position.getIsDeleted());
        verify(positionRepository, times(1)).findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId);
        verify(positionRepository, times(1)).save(position);
    }

    @Test
    void deletePosition_shouldThrowExceptionWhenNotFound() {
        when(positionRepository.findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> positionService.deletePosition(skillId, companyId));
        verify(positionRepository, times(1)).findBySkillIdAndCompanyIdAndIsDeletedFalse(skillId, companyId);
        verify(positionRepository, never()).save(any(Position.class));
    }
}
