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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    private Integer companyId;
    private Integer positionId;
    private Position position;

    @BeforeEach
    void setUp() {
        companyId = 1;
        positionId = 101;
        position = new Position();
        position.setPositionId(positionId);
        position.setCompanyId(companyId);
        position.setDescription("Server");
        position.setIsDeleted(false);
    }

    @Test
    void getPositions_Active_ReturnsActivePositions() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setIsDeleted(true);

        when(positionRepository.findByCompanyIdAndIsDeletedFalse(companyId))
                .thenReturn(Arrays.asList(position));

        List<PositionSummary> result = positionService.getPositions(companyId, "active");

        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(p -> p.positionId().equals(positionId)));
        verify(positionRepository, times(1)).findByCompanyIdAndIsDeletedFalse(companyId);
    }

    @Test
    void getPositions_Inactive_ReturnsInactivePositions() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setIsDeleted(true);
        deletedPosition.setDescription("Former Server");

        when(positionRepository.findByCompanyIdAndIsDeletedTrue(companyId))
                .thenReturn(Arrays.asList(deletedPosition));

        List<PositionSummary> result = positionService.getPositions(companyId, "inactive");

        assertEquals(1, result.size());
        assertEquals(102, result.get(0).positionId());
        verify(positionRepository, times(1)).findByCompanyIdAndIsDeletedTrue(companyId);
    }

    @Test
    void getPositions_All_ReturnsAllPositions() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setIsDeleted(true);

        when(positionRepository.findByCompanyId(companyId))
                .thenReturn(Arrays.asList(position, deletedPosition));

        List<PositionSummary> result = positionService.getPositions(companyId, "all");

        assertEquals(2, result.size());
        verify(positionRepository, times(1)).findByCompanyId(companyId);
    }

    @Test
    void getPositionById_ExistingActive_ReturnsPosition() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId))
                .thenReturn(Optional.of(position));

        Optional<PositionSummary> result = positionService.getPositionById(positionId, companyId);

        assertTrue(result.isPresent());
        assertEquals(positionId, result.get().positionId());
        verify(positionRepository, times(1)).findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId);
    }

    @Test
    void getPositionById_NotFound_ReturnsEmpty() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId))
                .thenReturn(Optional.empty());

        Optional<PositionSummary> result = positionService.getPositionById(positionId, companyId);

        assertFalse(result.isPresent());
        verify(positionRepository, times(1)).findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId);
    }

    @Test
    void createPosition_SavesPosition() {
        CreatePositionRequest request = new CreatePositionRequest(companyId, "Bartender");
        
        positionService.createPosition(request);

        verify(positionRepository, times(1)).save(any(Position.class));
    }

    @Test
    void updatePosition_Existing_UpdatesAndSaves() {
        UpdatePositionRequest request = new UpdatePositionRequest("Lead Server");
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId))
                .thenReturn(Optional.of(position));

        positionService.updatePosition(positionId, companyId, request);

        assertEquals("Lead Server", position.getDescription());
        verify(positionRepository, times(1)).save(position);
        verify(positionRepository, times(1)).findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId);
    }

    @Test
    void updatePosition_NotFound_ThrowsException() {
        UpdatePositionRequest request = new UpdatePositionRequest("Lead Server");
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> positionService.updatePosition(positionId, companyId, request));
        verify(positionRepository, times(1)).findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId);
    }

    @Test
    void deletePosition_Existing_SetsDeletedAndSaves() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId))
                .thenReturn(Optional.of(position));

        positionService.deletePosition(positionId, companyId);

        assertTrue(position.getIsDeleted());
        verify(positionRepository, times(1)).save(position);
        verify(positionRepository, times(1)).findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId);
    }

    @Test
    void deletePosition_NotFound_ThrowsException() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> positionService.deletePosition(positionId, companyId));
        verify(positionRepository, times(1)).findByPositionIdAndCompanyIdAndIsDeletedFalse(positionId, companyId);
    }
}
