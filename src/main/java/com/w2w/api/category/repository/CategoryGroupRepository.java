package com.w2w.api.category.repository;

import com.w2w.api.category.model.CategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryGroupRepository extends JpaRepository<CategoryGroup, Integer> {
    List<CategoryGroup> findByCompanyId(Integer companyId);
}
