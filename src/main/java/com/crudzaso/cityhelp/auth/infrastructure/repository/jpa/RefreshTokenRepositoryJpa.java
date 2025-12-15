package com.crudzaso.cityhelp.auth.infrastructure.repository.jpa;

import com.crudzaso.cityhelp.auth.infrastructure.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface for RefreshTokenEntity.
 * Provides database operations for RefreshToken domain entities.
 * Infrastructure layer - JPA specific interface.
 */
@Repository
@Transactional
public interface RefreshTokenRepositoryJpa extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    List<RefreshTokenEntity> findByUserId(Long userId);

    List<RefreshTokenEntity> findByUserIdAndIsRevokedFalse(Long userId);

    List<RefreshTokenEntity> findByExpiresAtBefore(LocalDateTime dateTime);

    boolean existsByToken(String token);

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.isRevoked = true WHERE rt.userId = :userId")
    int revokeAllTokensForUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :dateTime")
    int deleteExpiredTokens(@Param("dateTime") LocalDateTime dateTime);

    @Query("SELECT COUNT(rt) FROM RefreshTokenEntity rt WHERE rt.userId = :userId AND rt.isRevoked = false AND rt.expiresAt > :now")
    long countActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
