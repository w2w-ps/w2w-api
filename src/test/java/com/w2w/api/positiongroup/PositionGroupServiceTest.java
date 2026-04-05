package com.w2w.api.positiongroup;

import com.w2w.api.position.model.Position;
import com.w2w.api.position.repository.PositionRepository;
import com.w2w.api.positiongroup.dto.CreatePositionGroupRequest;
import com.w2w.api.positiongroup.dto.PositionGroupSummary;
import com.w2w.api.positiongroup.dto.UpdatePositionGroupRequest;
import com.w2w.api.positiongroup.model.PositionGroup;
import com.w2w.api.positiongroup.repository.PositionGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private Position server;
    private Position bartender;
    private PositionGroup positionGroup;

    @BeforeEach
    void setUp() {
        server = new Position();
        server.setSkillId(101);
        server.setCompanyId(1);
        server.setDescription("Server");
        server.setIsDeleted(false);
        server.setTimestamp(LocalDateTime.now());

        bartender = new Position();
        bartender.setSkillId(102);
        bartender.setCompanyId(1);
        bartender.setDescription("Bartender");
        bartender.setIsDeleted(false);
        bartender.setTimestamp(LocalDateTime.now());

        positionGroup = new PositionGroup();
        positionGroup.setGroupId(201);
        positionGroup.setCompanyId(1);
        positionGroup.setDescription("Front of House");
        positionGroup.setPositions(List.of(server));
    }

    @Test
    void getPositionGroups_returnsMappedGroups() {
        when(positionGroupRepository.findByCompanyId(1)).thenReturn(List.of(positionGroup));

        List<PositionGroupSummary> result = positionGroupService.getPositionGroups(1, "all");

        assertEquals(1, result.size());
        assertEquals(201, result.get(0).id());
        assertEquals("Front of House", result.get(0).name());
        assertEquals(1, result.get(0).positions().size());
        assertEquals(101, result.get(0).positions().get(0).positionId());
        verify(positionGroupRepository).findByCompanyId(1);
    }

    @Test
    void getPositionGroups_returnsMappedActiveGroups() {
        when(positionGroupRepository.findByCompanyIdAndIsDeletedFalse(1)).thenReturn(List.of(positionGroup));

        List<PositionGroupSummary> result = positionGroupService.getPositionGroups(1, "active");

        assertEquals(1, result.size());
        assertEquals(201, result.get(0).id());
        verify(positionGroupRepository).findByCompanyIdAndIsDeletedFalse(1);
    }

    @Test
    void getPositionGroups_returnsMappedInactiveGroups() {
        positionGroup.setIsDeleted(true);
        when(positionGroupRepository.findByCompanyIdAndIsDeletedTrue(1)).thenReturn(List.of(positionGroup));

        List<PositionGroupSummary> result = positionGroupService.getPositionGroups(1, "inactive");

        assertEquals(1, result.size());
        assertEquals(201, result.get(0).id());
        verify(positionGroupRepository).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getPositionGroups_throwsWhenStatusUnsupported() {
        assertThrows(ResponseStatusException.class, () -> positionGroupService.getPositionGroups(1, "archived"));

        verify(positionGroupRepository, never()).findByCompanyId(1);
        verify(positionGroupRepository, never()).findByCompanyIdAndIsDeletedFalse(1);
        verify(positionGroupRepository, never()).findByCompanyIdAndIsDeletedTrue(1);
    }

    @Test
    void getPositionGroupById_returnsMappedGroup() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.of(positionGroup));

        Optional<PositionGroupSummary> result = positionGroupService.getPositionGroupById(201, 1);

        assertTrue(result.isPresent());
        assertEquals("Front of House", result.get().name());
        verify(positionGroupRepository).findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1);
    }

    @Test
    void createPositionGroup_savesResolvedPositions() {
        CreatePositionGroupRequest request = new CreatePositionGroupRequest(1, "Front of House", List.of(102, 101));
        when(positionRepository.findBySkillIdInAndCompanyId(
                argThat(positionIds -> positionIds.size() == 2 && positionIds.containsAll(List.of(102, 101))),
                eq(1)
        ))
                .thenReturn(List.of(server, bartender));

        positionGroupService.createPositionGroup(request);

        verify(positionRepository).findBySkillIdInAndCompanyId(
                argThat(positionIds -> positionIds.size() == 2 && positionIds.containsAll(List.of(102, 101))),
                eq(1)
        );
        verify(positionGroupRepository).save(argThat(group ->
                group.getCompanyId().equals(1)
                        && group.getDescription().equals("Front of House")
                        && Boolean.FALSE.equals(group.getIsDeleted())
                        && group.getPositions().stream().map(Position::getSkillId).toList().equals(List.of(102, 101))));
    }

    @Test
    void createPositionGroup_throwsWhenAnyPositionIsMissing() {
        CreatePositionGroupRequest request = new CreatePositionGroupRequest(1, "Front of House", List.of(101, 999));
        when(positionRepository.findBySkillIdInAndCompanyId(
                argThat(positionIds -> positionIds.size() == 2 && positionIds.containsAll(List.of(101, 999))),
                eq(1)
        ))
                .thenReturn(List.of(server));

        assertThrows(ResponseStatusException.class, () -> positionGroupService.createPositionGroup(request));

        verify(positionGroupRepository, never()).save(any(PositionGroup.class));
    }

    @Test
    void updatePositionGroup_replacesGroupMembership() {
        UpdatePositionGroupRequest request = new UpdatePositionGroupRequest("Updated Front of House", List.of(102));
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.of(positionGroup));
        when(positionRepository.findBySkillIdInAndCompanyId(
                argThat(positionIds -> positionIds.size() == 1 && positionIds.contains(102)),
                eq(1)
        )).thenReturn(List.of(bartender));

        positionGroupService.updatePositionGroup(201, 1, request);

        assertEquals("Updated Front of House", positionGroup.getDescription());
        assertEquals(1, positionGroup.getPositions().size());
        assertEquals(102, positionGroup.getPositions().get(0).getSkillId());
        verify(positionGroupRepository).save(positionGroup);
    }

    @Test
    void deletePositionGroup_softDeletesGroup() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.of(positionGroup));

        positionGroupService.deletePositionGroup(201, 1);

        assertTrue(positionGroup.getIsDeleted());
        verify(positionGroupRepository).save(positionGroup);
    }

    @Test
    void deletePositionGroup_throwsWhenMissing() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(201, 1)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> positionGroupService.deletePositionGroup(201, 1));

        verify(positionGroupRepository, never()).save(any(PositionGroup.class));
    }
}
