package com.crudzaso.cityhelp.auth.infrastructure.repository.jpa;

import com.crudzaso.cityhelp.auth.infrastructure.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepositoryJpa extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findByToken(String token);

    @Query("DELETE FROM PasswordResetTokenEntity p WHERE p.userId = ?1")
    void deleteByUserId(Long userId);

    @Query("DELETE FROM PasswordResetTokenEntity p WHERE p.expiresAt < ?1")
    void deleteExpiredTokens(LocalDateTime now);
}
