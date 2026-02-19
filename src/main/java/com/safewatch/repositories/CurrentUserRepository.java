package com.safewatch.repositories;

import com.safewatch.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CurrentUserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String username);

    boolean existsByEmail(String email);
}
