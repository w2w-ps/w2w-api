package com.w2w.api.position.repository;

import com.w2w.api.position.model.SkillGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SkillGroupRepository extends JpaRepository<SkillGroup, Integer> {
    List<SkillGroup> findByCompanyId(Integer companyId);
}
