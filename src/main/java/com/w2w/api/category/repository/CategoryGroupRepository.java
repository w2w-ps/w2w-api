package com.w2w.api.category.repository;

import com.w2w.api.category.model.CategoryGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, Integer> {

    @EntityGraph(attributePaths = "categories")
    List<CategoryGroup> findByCompanyId(Integer companyId);

    List<CategoryGroup> findByCompanyIdAndIsDeletedFalse(Integer companyId);

    List<CategoryGroup> findByCompanyIdAndIsDeletedTrue(Integer companyId);

    Optional<CategoryGroup> findByGroupIdAndCompanyIdAndIsDeletedFalse(Integer groupId, Integer companyId);
}
