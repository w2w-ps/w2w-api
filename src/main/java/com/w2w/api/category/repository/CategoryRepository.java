package com.w2w.api.category.repository;

import com.w2w.api.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByCompanyId(Integer companyId);
    List<Category> findByCompanyIdAndIsDeletedFalse(Integer companyId);
    List<Category> findByCompanyIdAndIsDeletedTrue(Integer companyId);
    Optional<Category> findByCategoryIdAndCompanyIdAndIsDeletedFalse(Integer categoryId, Integer companyId);
}
