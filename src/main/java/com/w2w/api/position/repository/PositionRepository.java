package com.w2w.api.position.repository;

import com.w2w.api.position.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Integer> {
    List<Position> findByCompanyId(Integer companyId);

    List<Position> findByCompanyIdAndIsDeletedFalse(Integer companyId);

    List<Position> findByCompanyIdAndIsDeletedTrue(Integer companyId);

    Optional<Position> findByPositionIdAndCompanyIdAndIsDeletedFalse(Integer positionId, Integer companyId);

    List<Position> findByPositionIdInAndCompanyId(Collection<Integer> positionIds, Integer companyId);
}
