package com.w2w.api.positiongroup;

import com.w2w.api.config.TenantContext;
import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.config.exception.ResourceNotFoundException;
import com.w2w.api.positiongroup.dto.PositionGroupSummary;
import com.w2w.api.positiongroup.dto.UpdatePositionGroupRequest;
import com.w2w.api.positiongroup.model.PositionGroup;
import com.w2w.api.positiongroup.repository.PositionGroupRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionGroupServiceTest {

    @Mock
    private PositionGroupRepository positionGroupRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionGroupService positionGroupService;

    private Position position;
    private PositionGroup positionGroup;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenant(1);

        position = new Position();
        position.setPositionId(101);
        position.setCompanyId(1);
        position.setDescription("Server");

        positionGroup = new PositionGroup();
        positionGroup.setGroupId(201);
        positionGroup.setCompanyId(1);
        positionGroup.setDescription("Front of House");
        positionGroup.setPositions(List.of(position));
        positionGroup.setIsDeleted(false);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void getPositionGroups_returnsMappedGroups() {
        when(positionGroupRepository.findByCompanyId(1)).thenReturn(List.of(positionGroup));

        List<PositionGroupSummary> result = positionGroupService.getPositionGroups("all");

        assertEquals(1, result.size());
        assertEquals("Front of House", result.getFirst().name());
    }

    @Test
    void getPositionGroupById_returnsMappedGroup() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.of(positionGroup));

        Optional<PositionGroupSummary> result = positionGroupService.getPositionGroupById(201);

        assertTrue(result.isPresent());
        assertEquals("Front of House", result.get().name());
    }

    @Test
    void createPositionGroup_savesResolvedPositions() {
        Position bartender = new Position();
        bartender.setPositionId(102);
        bartender.setCompanyId(1);
        bartender.setDescription("Bartender");

        when(positionRepository.findByPositionIdInAndCompanyId(any(), eq(1))).thenReturn(List.of(position, bartender));

        positionGroupService.createPositionGroup("Front of House", List.of(102, 101));

        ArgumentCaptor<PositionGroup> captor = ArgumentCaptor.forClass(PositionGroup.class);
        verify(positionGroupRepository).save(captor.capture());
        assertEquals("Front of House", captor.getValue().getDescription());
        assertEquals(102, captor.getValue().getPositions().get(0).getPositionId());
        assertEquals(101, captor.getValue().getPositions().get(1).getPositionId());
    }

    @Test
    void createPositionGroup_missingPosition_throwsBadRequest() {
        when(positionRepository.findByPositionIdInAndCompanyId(any(), eq(1))).thenReturn(List.of(position));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> positionGroupService.createPositionGroup("Front of House", List.of(101, 999))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(positionGroupRepository, never()).save(any(PositionGroup.class));
    }

    @Test
    void updatePositionGroup_updatesDescriptionAndMembership() {
        Position bartender = new Position();
        bartender.setPositionId(102);
        bartender.setCompanyId(1);
        bartender.setDescription("Bartender");

        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.of(positionGroup));
        when(positionRepository.findByPositionIdInAndCompanyId(any(), eq(1))).thenReturn(List.of(position, bartender));

        positionGroupService.updatePositionGroup(201, new UpdatePositionGroupRequest("Updated Group", List.of(102, 101)));

        assertEquals("Updated Group", positionGroup.getDescription());
        assertEquals(102, positionGroup.getPositions().get(0).getPositionId());
        verify(positionGroupRepository).save(positionGroup);
    }

    @Test
    void deletePositionGroup_setsDeletedFlag() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.of(positionGroup));

        positionGroupService.deletePositionGroup(201);

        assertTrue(positionGroup.getIsDeleted());
        verify(positionGroupRepository).save(positionGroup);
    }

    @Test
    void deletePositionGroup_missingGroup_throwsNotFound() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> positionGroupService.deletePositionGroup(201));

        verify(positionGroupRepository, never()).save(any(PositionGroup.class));
    }

    @Test
    void createPositionGroup_withoutTenant_throwsUnauthorized() {
        TenantContext.clear();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> positionGroupService.createPositionGroup("Front of House", List.of(101))
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(positionGroupRepository, never()).save(any(PositionGroup.class));
    }
}
