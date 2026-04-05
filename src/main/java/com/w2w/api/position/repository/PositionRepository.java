package com.w2w.api.position.repository;

import com.w2w.api.position.model.Position; // Import Position entity
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional; // Needed for findById methods

@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    // Custom queries for Position, considering isDeleted
    List<Position> findByCompanyIdAndIsDeletedFalse(Integer companyId);
    Optional<Position> findBySkillIdAndCompanyIdAndIsDeletedFalse(Integer skillId, Integer companyId);

    List<Position> findByCompanyIdAndIsDeletedTrue(Integer companyId);
    List<Position> findByCompanyId(Integer companyId); // New method to get all positions by companyId
    List<Position> findBySkillIdInAndCompanyId(Collection<Integer> skillIds, Integer companyId);

    // TODO: Consider if there's a need for more nuanced 'active'/'inactive' states beyond just isDeleted
}
