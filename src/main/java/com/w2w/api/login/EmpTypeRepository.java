package com.w2w.api.login;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpTypeRepository extends JpaRepository<EmpType, Integer> {
    Optional<EmpType> findByName(String name);

    List<EmpType> findAllByOrderBySortOrderAsc();
}
