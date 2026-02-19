package com.safewatch.repositories;

import com.safewatch.models.RoleType;
import com.safewatch.models.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByRoleName(RoleType roleType);
}
