package com.example.miniboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.miniboard.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // username은 unique 인덱스를 걸어둬서 해당 조회가 풀스캔이 아님
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}