package com.w2w.api.position;

import com.w2w.api.config.TenantContext;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.dto.UpdatePositionRequest;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void getPositions_active_returnsActivePositions() {
        when(positionRepository.findByCompanyIdAndIsDeletedFalse(1)).thenReturn(List.of(position));

        List<PositionSummary> result = positionService.getPositions("active");

        assertEquals(1, result.size());
        assertEquals(101, result.getFirst().positionId());
    }

    @Test
    void getPositions_inactive_returnsInactivePositions() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setCompanyId(1);
        deletedPosition.setDescription("Former Server");
        deletedPosition.setIsDeleted(true);
        when(positionRepository.findByCompanyIdAndIsDeletedTrue(1)).thenReturn(List.of(deletedPosition));

        List<PositionSummary> result = positionService.getPositions("inactive");

        assertEquals(1, result.size());
        assertEquals(102, result.getFirst().positionId());
    }

    @Test
    void getPositions_all_returnsAllPositions() {
        Position deletedPosition = new Position();
        deletedPosition.setPositionId(102);
        deletedPosition.setCompanyId(1);
        deletedPosition.setIsDeleted(true);
        when(positionRepository.findByCompanyId(1)).thenReturn(List.of(position, deletedPosition));

        List<PositionSummary> result = positionService.getPositions("all");

        assertEquals(2, result.size());
        verify(positionRepository).findByCompanyId(1);
    }

    @Test
    void getPositions_unsupportedStatus_throwsBadRequest() {
        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> positionService.getPositions("archived"));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getPositionById_returnsPosition() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(101, 1)).thenReturn(Optional.of(position));

        Optional<PositionSummary> result = positionService.getPositionById(101);

        assertTrue(result.isPresent());
        assertEquals(101, result.get().positionId());
    }

    @Test
    void createPosition_savesPosition() {
        positionService.createPosition("Bartender");

        verify(positionRepository).save(any(Position.class));
    }

    @Test
    void createPosition_withoutTenant_throwsUnauthorized() {
        TenantContext.clear();

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> positionService.createPosition("Bartender"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(positionRepository, never()).save(any(Position.class));
    }

    @Test
    void updatePosition_updatesAndSaves() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(101, 1)).thenReturn(Optional.of(position));

        positionService.updatePosition(101, new UpdatePositionRequest("Lead Server"));

        assertEquals("Lead Server", position.getDescription());
        verify(positionRepository).save(position);
    }

    @Test
    void deletePosition_setsDeletedAndSaves() {
        when(positionRepository.findByPositionIdAndCompanyIdAndIsDeletedFalse(101, 1)).thenReturn(Optional.of(position));

        positionService.deletePosition(101);

        assertTrue(position.getIsDeleted());
        verify(positionRepository).save(position);
    }
}
