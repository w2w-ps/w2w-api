package com.w2w.api.login;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoginRepository extends JpaRepository<User, Integer> {

    Optional<User> findByLoginId(String loginId);

    @Query("""
            SELECT u.password AS password
            FROM User u
            WHERE u.loginId = :loginId
            """)
    Optional<String> findPasswordByLoginId(@Param("loginId") String loginId);

    @Query("""
            SELECT u.companyId AS companyId, userRole.name AS roleName
            FROM User u
            LEFT JOIN u.role userRole
            WHERE u.loginId = :loginId
            """)
    Optional<AuthContextProjection> findAuthContextByLoginId(@Param("loginId") String loginId);

    List<User> findByCompanyIdAndRoleName(Integer companyId, String roleName);
}
