package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
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
import java.util.stream.Collectors;

/**
 * JPA interface for EmailVerificationCode operations.
 */
@Repository
@Transactional
public interface EmailVerificationRepositoryJpa extends JpaRepository<EmailVerificationCodeEntity, Long> {

    Optional<EmailVerificationCodeEntity> findByUserIdAndIsUsedFalse(Long userId);

    Optional<EmailVerificationCodeEntity> findByUserIdAndCodeAndIsUsedFalse(Long userId, String code);

    List<EmailVerificationCodeEntity> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationCodeEntity e WHERE e.userId = :userId AND e.isUsed = true")
    void deleteUsedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM EmailVerificationCodeEntity e WHERE e.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailVerificationCodeEntity e SET e.isUsed = true WHERE e.id = :id")
    void markAsUsed(@Param("id") Long id);
}

/**
 * Infrastructure implementation of EmailVerificationRepository interface.
 * Implements domain repository contract using JPA.
 *
 * Business Rules:
 * - Convert between JPA entities and domain models
 * - Handle database operations with proper error handling
 * - Maintain clean architecture separation
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
@Repository
public class EmailVerificationRepositoryImpl implements EmailVerificationRepository {

    private final EmailVerificationRepositoryJpa jpaRepository;

    public EmailVerificationRepositoryImpl(EmailVerificationRepositoryJpa jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EmailVerificationCode save(EmailVerificationCode emailVerificationCode) {
        EmailVerificationCodeEntity entity = toEntity(emailVerificationCode);
        EmailVerificationCodeEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<EmailVerificationCode> findLatestByUserId(Long userId) {
        return jpaRepository.findByUserIdAndIsUsedFalse(userId)
                .map(this::toDomain);
    }

    @Override
    public Optional<EmailVerificationCode> findByUserIdAndCode(Long userId, String code) {
        return jpaRepository.findByUserIdAndCodeAndIsUsedFalse(userId, code)
                .map(this::toDomain);
    }

    @Override
    public List<EmailVerificationCode> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsUsed(Long id) {
        jpaRepository.markAsUsed(id);
    }

    @Override
    public void deleteUsedByUserId(Long userId) {
        jpaRepository.deleteUsedByUserId(userId);
    }

    @Override
    public void deleteExpired() {
        jpaRepository.deleteExpired(LocalDateTime.now());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).size();
    }

    @Override
    public long countUnused() {
        return jpaRepository.findAll().stream()
                .filter(entity -> !entity.getIsUsed())
                .count();
    }

    @Override
    public long countExpired() {
        LocalDateTime now = LocalDateTime.now();
        return jpaRepository.findAll().stream()
                .filter(entity -> entity.getExpiresAt().isBefore(now))
                .count();
    }

    private EmailVerificationCodeEntity toEntity(EmailVerificationCode domain) {
        return EmailVerificationCodeEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .code(domain.getCode())
                .expiresAt(domain.getExpiresAt())
                .isUsed(domain.getIsUsed())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private EmailVerificationCode toDomain(EmailVerificationCodeEntity entity) {
        return EmailVerificationCode.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .code(entity.getCode())
                .expiresAt(entity.getExpiresAt())
                .isUsed(entity.getIsUsed())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}