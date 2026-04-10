package com.w2w.api.position;

import com.w2w.api.config.TenantContext;
import com.w2w.api.config.exception.ResourceNotFoundException;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionService positionService;

    private Position position;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1);

        position = new Position();
        position.setPositionId(101);
        position.setCompanyId(1);
        position.setDescription("Server");
        position.setIsDeleted(false);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getPositions_active_returnsActive() {
        when(positionRepository.findByCompanyIdAndIsDeletedFalse(1)).thenReturn(List.of(position));

        List<PositionSummary> result = positionService.get("active");

        assertEquals(1, result.size());
        assertEquals(101, result.getFirst().positionId());
    }

    @Test
    void getPositions_inactive_returnsInactive() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setCompanyId(1);
        deletedPosition.setDescription("Former Server");
        deletedPosition.setIsDeleted(true);
        when(positionRepository.findByCompanyIdAndIsDeletedTrue(1)).thenReturn(List.of(deletedPosition));

        List<PositionSummary> result = positionService.get("inactive");

        assertEquals(1, result.size());
        assertEquals(102, result.getFirst().positionId());
    }

    @Test
    void getPositions_all_returnsAll() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setCompanyId(1);
        deletedPosition.setIsDeleted(true);
        when(positionRepository.findByCompanyId(1)).thenReturn(List.of(position, deletedPosition));

        List<PositionSummary> result = positionService.get("all");

        assertEquals(2, result.size());
        verify(positionRepository).findByCompanyId(1);
    }

    @Test
    void get_unsupportedStatus_throwsBadRequest() {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> positionService.get("archived"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Invalid status. Allowed values: all, active, inactive.", exception.getReason());
    }

    @Test
    void getPositionById_returnsSummary() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        Optional<PositionSummary> result = positionService.getPositionSummaryById(101);

        assertTrue(result.isPresent());
        assertEquals(101, result.get().positionId());
    }

    @Test
    void createPosition_saves() {
        positionService.create("Bartender");

        ArgumentCaptor<Position> positionCaptor = ArgumentCaptor.forClass(Position.class);

        verify(positionRepository).save(positionCaptor.capture());
        assertEquals(1, positionCaptor.getValue().getCompanyId());
    }

    @Test
    void update_updatesAndSaves() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        positionService.update(101, new UpdatePositionRequest("Lead Server"));

        assertEquals("Lead Server", position.getDescription());
        verify(positionRepository).save(position);
    }

    @Test
    void delete_setsDeletedAndSaves() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        positionService.delete(101);

        assertTrue(position.getIsDeleted());
        verify(positionRepository).save(position);
    }

    @Test
    void update_notFound_throwsNotFound() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.update(101, new UpdatePositionRequest("Lead Server"))
        );

        assertEquals("Position not found", exception.getMessage());
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.delete(101)
        );

        assertEquals("Position not found", exception.getMessage());
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void updatePosition_deleted_throwsNotFound() {
        position.setIsDeleted(true);
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.update(101, new UpdatePositionRequest("Lead Server"))
        );

        assertEquals("Position not found", exception.getMessage());
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void delete_alreadyDeleted_returnsWithoutSaving() {
        position.setIsDeleted(true);
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        positionService.delete(101);

        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void restorePosition_deleted_restoresAndSaves() {
        position.setIsDeleted(true);
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        positionService.restore(101);

        assertEquals(Boolean.FALSE, position.getIsDeleted());
        verify(positionRepository).save(position);
    }

    @Test
    void restorePosition_active_returnsWithoutSaving() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.of(position));

        positionService.restore(101);

        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void restore_notFound_throwsNotFound() {
        when(positionRepository.findByPositionIdAndCompanyId(101, 1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> positionService.restore(101)
        );

        assertEquals("Position not found", exception.getMessage());
        verify(positionRepository, never()).save(any(Position.class));
    }
}
