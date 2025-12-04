package com.crudzaso.cityhelp.auth.infrastructure.repository.jpa;

import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.infrastructure.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository interface for UserEntity.
 * Provides database operations for User domain entities.
 * Infrastructure layer - JPA specific interface.
 */
@Repository
@Transactional
public interface UserRepositoryJpa extends JpaRepository<UserEntity, Long> {

    // Query methods following Spring Data naming conventions
    Optional<UserEntity> findByUuid(UUID uuid);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findByRole(UserRole role);

    List<UserEntity> findByStatus(UserStatus status);

    List<UserEntity> findByOauthProvider(OAuthProvider provider);

    List<UserEntity> findByStatusAndIsVerified(UserStatus status, boolean isVerified);

    List<UserEntity> findByStatusAndOauthProvider(UserStatus status, OAuthProvider provider);

    // Update operations with explicit queries
    @Modifying
    @Query("UPDATE UserEntity u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    int updateLastLoginAtById(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    @Modifying
    @Query("UPDATE UserEntity u SET u.isVerified = :isVerified WHERE u.id = :userId")
    int updateIsVerifiedById(@Param("userId") Long userId, @Param("isVerified") boolean isVerified);

    @Modifying
    @Query("UPDATE UserEntity u SET u.status = :status WHERE u.id = :userId")
    int updateStatusById(@Param("userId") Long userId, @Param("status") UserStatus status);
}
