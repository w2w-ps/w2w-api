package com.w2w.api.manager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManagerPermissionsRepository extends JpaRepository<ManagerPermissions, Integer> {
    Optional<ManagerPermissions> findByUserId(Integer userId);
}
