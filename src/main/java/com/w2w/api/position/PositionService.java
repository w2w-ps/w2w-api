package com.w2w.api.position;

import com.w2w.api.position.dto.PositionGroupSummary;
import com.w2w.api.position.dto.PositionSummary;
import com.w2w.api.position.repository.SkillGroupRepository;
import com.w2w.api.position.repository.SkillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PositionService {
    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private SkillGroupRepository skillGroupRepository;

    public List<PositionSummary> getPositionsByCompanyId(Integer companyId) {
        return skillRepository.findByCompanyId(companyId).stream()
                .map(skill -> new PositionSummary(skill.getSkillId(), skill.getDescription()))
                .collect(Collectors.toList());
    }

    public List<PositionGroupSummary> getPositionGroupsByCompanyId(Integer companyId) {
        return skillGroupRepository.findByCompanyId(companyId).stream()
                .map(group -> new PositionGroupSummary(
                        group.getGroupId(),
                        group.getDescription(),
                        group.getSkills().stream()
                                .map(skill -> new PositionSummary(skill.getSkillId(), skill.getDescription()))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }
}
