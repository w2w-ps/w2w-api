package com.w2w.api.positiongroup.repository;

import com.w2w.api.positiongroup.model.PositionGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionGroupRepository extends JpaRepository<PositionGroup, Integer> {

    @EntityGraph(attributePaths = "positions")
    List<PositionGroup> findByCompanyId(Integer companyId);

    @EntityGraph(attributePaths = "positions")
    List<PositionGroup> findByCompanyIdAndIsDeletedFalse(Integer companyId);

    @EntityGraph(attributePaths = "positions")
    List<PositionGroup> findByCompanyIdAndIsDeletedTrue(Integer companyId);

    @EntityGraph(attributePaths = "positions")
    Optional<PositionGroup> findByGroupIdAndCompanyIdAndIsDeletedFalse(Integer groupId, Integer companyId);
}
