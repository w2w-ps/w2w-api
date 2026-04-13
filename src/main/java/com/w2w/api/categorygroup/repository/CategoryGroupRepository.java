package com.w2w.api.categorygroup.repository;

import com.w2w.api.categorygroup.model.CategoryGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, Integer> {

    @EntityGraph(attributePaths = "categories")
    List<CategoryGroup> findByCompanyId(Integer companyId);

    @EntityGraph(attributePaths = "categories")
    List<CategoryGroup> findByCompanyIdAndIsDeletedFalse(Integer companyId);

    @EntityGraph(attributePaths = "categories")
    List<CategoryGroup> findByCompanyIdAndIsDeletedTrue(Integer companyId);

    @EntityGraph(attributePaths = "categories")
    Optional<CategoryGroup> findByGroupIdAndCompanyIdAndIsDeletedFalse(Integer groupId, Integer companyId);
}
