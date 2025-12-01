package com.crudzaso.cityhelp.auth.infrastructure.repository.jpa;

import com.crudzaso.cityhelp.auth.infrastructure.entity.EmailVerificationCodeEntity;
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
 * Spring Data JPA repository interface for EmailVerificationCodeEntity.
 * Provides database operations for EmailVerificationCode domain entities.
 * Infrastructure layer - JPA specific interface.
 */
@Repository
@Transactional
public interface EmailVerificationRepositoryJpa extends JpaRepository<EmailVerificationCodeEntity, Long> {

    Optional<EmailVerificationCodeEntity> findByUserIdAndIsUsedFalse(Long userId);

    Optional<EmailVerificationCodeEntity> findByUserIdAndCodeAndIsUsedFalse(Long userId, String code);

    List<EmailVerificationCodeEntity> findByUserId(Long userId);

    List<EmailVerificationCodeEntity> findByIsUsedFalse();

    List<EmailVerificationCodeEntity> findByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationCodeEntity e WHERE e.userId = :userId AND e.isUsed = true")
    void deleteUsedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationCodeEntity e WHERE e.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailVerificationCodeEntity e SET e.isUsed = true WHERE e.id = :id")
    void markAsUsed(@Param("id") Long id);

    @Query("SELECT COUNT(e) FROM EmailVerificationCodeEntity e WHERE e.isUsed = false")
    long countUnused();

    @Query("SELECT COUNT(e) FROM EmailVerificationCodeEntity e WHERE e.expiresAt < :now")
    long countExpired(@Param("now") LocalDateTime now);
}
