package com.w2w.api.login;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginRepository extends JpaRepository<User, Integer> {

    Optional<User> findByLoginId(String loginId);
}
