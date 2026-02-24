package com.safewatch.repositories;

import com.safewatch.models.RefreshToken;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepo extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String accessToken);

    List<RefreshToken> findAllBySessionId(UUID sessionId);

    @Query("SELECT count(t) FROM RefreshToken t WHERE t.userId = :userId AND t.revokedAt IS NULL AND t.expiresAt > :now")
    long countActive(@Param("userId") Long userId, @Param("now") Instant now);

    @Query("SELECT t FROM RefreshToken t WHERE t.userId = :userId and t.revokedAt IS NULL AND t.expiresAt > :now ORDER BY t.createdAt ASC")
    List<RefreshToken> findOldestToken(@Param("userId") long userId, @Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.id IN :ids AND t.revokedAt IS NULL")
    int revokeByIds(@Param("ids") List<Long> ids, @Param("now") Instant now);
}
