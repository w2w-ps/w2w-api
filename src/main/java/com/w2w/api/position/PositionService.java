package com.w2w.api.position;

import com.w2w.api.position.dto.PositionDto;
import com.w2w.api.position.dto.PositionGroupDto;
import com.w2w.api.position.repository.SkillGroupRepository;
import com.w2w.api.position.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PositionService {
    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillGroupRepository skillGroupRepository;

    public List<PositionDto> getPositionsByCompanyId(Integer companyId) {
        return skillRepository.findByCompanyId(companyId).stream()
                .map(skill -> new PositionDto(skill.getSkillId(), skill.getDescription()))
                .collect(Collectors.toList());
    }

    public List<PositionGroupDto> getPositionGroupsByCompanyId(Integer companyId) {
        return skillGroupRepository.findByCompanyId(companyId).stream()
                .map(group -> new PositionGroupDto(
                        group.getGroupId(),
                        group.getDescription(),
                        group.getSkills().stream()
                                .map(skill -> new PositionDto(skill.getSkillId(), skill.getDescription()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
