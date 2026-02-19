package com.safewatch.repositories;

import com.safewatch.models.TokenType;
import com.safewatch.models.User;
import com.safewatch.models.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByTokenHashAndTokenTypeAndUsedFalse(String tokenHash, TokenType tokenType);

    Optional<VerificationToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
                update VerificationToken t
                   set t.isUsed = true
                 where t.user = :user
                   and t.tokenType = :type
                   and t.isUsed = false
            """)
    int invalidateActiveTokens(@Param("user") User user, @Param("type") TokenType type);

    @Modifying
    @Query("""
                delete from VerificationToken t
                 where t.expiresAt < :now
            """)
    int deleteExpired(@Param("now") OffsetDateTime now);

    Optional<VerificationToken> findById(UUID token);
}
