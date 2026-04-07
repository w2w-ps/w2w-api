package com.w2w.api.positiongroup;

import com.w2w.api.position.dto.PositionSummary;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PositionGroupServiceTest {

    @Mock
    private PositionGroupRepository positionGroupRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PositionGroupService positionGroupService;

    private Integer companyId;
    private Position position;
    private PositionGroup group;

    @BeforeEach
    void setUp() {
        companyId = 1;

        position = new Position();
        position.setPositionId(101);
        position.setCompanyId(companyId);
        position.setDescription("Server");

        group = new PositionGroup();
        group.setGroupId(1);
        group.setCompanyId(companyId);
        group.setDescription("Core Staff");
        group.setPositions(Collections.singletonList(position));
    }

    @Test
    void getPositionGroups_ReturnsSummaries() {
        when(positionGroupRepository.findByCompanyId(companyId))
                .thenReturn(Collections.singletonList(group));

        List<PositionGroupSummary> result = positionGroupService.getPositionGroups(companyId, "all");

        assertEquals(1, result.size());
        assertEquals("Core Staff", result.get(0).name());
        assertEquals(1, result.get(0).positions().size());
        assertEquals("Server", result.get(0).positions().get(0).description());
    }

    @Test
    void createPositionGroup_ResolvesAndSaves() {
        CreatePositionGroupRequest request = new CreatePositionGroupRequest(
                companyId,
                "Front of House",
                Arrays.asList(101, 102)
        );

        Position server = new Position();
        server.setPositionId(101);
        server.setDescription("Server");

        Position bartender = new Position();
        bartender.setPositionId(102);
        bartender.setDescription("Bartender");

        when(positionRepository.findByPositionIdInAndCompanyId(anySet(), eq(companyId)))
                .thenReturn(Arrays.asList(server, bartender));

        positionGroupService.createPositionGroup(request);

        ArgumentCaptor<PositionGroup> groupCaptor = ArgumentCaptor.forClass(PositionGroup.class);
        verify(positionGroupRepository).save(groupCaptor.capture());

        PositionGroup savedGroup = groupCaptor.getValue();
        assertEquals("Front of House", savedGroup.getDescription());
        assertEquals(2, savedGroup.getPositions().size());
        // Verify order is preserved from request
        assertEquals(101, savedGroup.getPositions().get(0).getPositionId());
        assertEquals(102, savedGroup.getPositions().get(1).getPositionId());
    }

    @Test
    void createPositionGroup_InvalidPositionId_ThrowsException() {
        CreatePositionGroupRequest request = new CreatePositionGroupRequest(
                companyId,
                "Invalid Group",
                Collections.singletonList(999)
        );

        when(positionRepository.findByPositionIdInAndCompanyId(anySet(), eq(companyId)))
                .thenReturn(Collections.emptyList());

        assertThrows(ResponseStatusException.class, () -> positionGroupService.createPositionGroup(request));
    }

    @Test
    void updatePositionGroup_UpdatesPositionsAndDescription() {
        UpdatePositionGroupRequest request = new UpdatePositionGroupRequest(
                "Updated Group",
                Arrays.asList(102, 101)
        );

        Position server = new Position();
        server.setPositionId(101);
        server.setDescription("Server");

        Position bartender = new Position();
        bartender.setPositionId(102);
        bartender.setDescription("Bartender");

        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(1, companyId))
                .thenReturn(Optional.of(group));
        when(positionRepository.findByPositionIdInAndCompanyId(anySet(), eq(companyId)))
                .thenReturn(Arrays.asList(server, bartender));

        positionGroupService.updatePositionGroup(1, companyId, request);

        verify(positionGroupRepository).save(group);
        assertEquals("Updated Group", group.getDescription());
        assertEquals(2, group.getPositions().size());
        assertEquals(102, group.getPositions().get(0).getPositionId());
        assertEquals(101, group.getPositions().get(1).getPositionId());
    }

    @Test
    void deletePositionGroup_SetsDeletedFlag() {
        when(positionGroupRepository.findByGroupIdAndCompanyIdAndIsDeletedFalse(1, companyId))
                .thenReturn(Optional.of(group));

        positionGroupService.deletePositionGroup(1, companyId);

        assertTrue(group.getIsDeleted());
        verify(positionGroupRepository).save(group);
    }
}
