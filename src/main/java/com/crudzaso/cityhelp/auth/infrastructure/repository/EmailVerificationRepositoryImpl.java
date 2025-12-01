package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.infrastructure.repository.EmailVerificationRepositoryJpa;

import java.util.Optional;

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

    private final EmailVerificationRepositoryJpa emailVerificationRepositoryJpa;

    public EmailVerificationRepositoryImpl(EmailVerificationRepositoryJpa emailVerificationRepositoryJpa) {
        this.emailVerificationRepositoryJpa = emailVerificationRepositoryJpa;
    }

    @Override
    public EmailVerificationCode save(EmailVerificationCode emailCode) {
        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity(emailCode);
        EmailVerificationCodeEntity savedEntity = emailVerificationRepositoryJpa.save(entity);
        return savedEntity.toDomainModel();
    }

    @Override
    public Optional<EmailVerificationCode> findById(Long id) {
        return emailVerificationRepositoryJpa.findById(id)
                .map(EmailVerificationCodeEntity::toDomainModel);
    }

    @Override
    public Optional<EmailVerificationCode> findByUserId(Long userId) {
        return emailVerificationRepositoryJpa.findByUserIdAndIsUsedFalse(userId)
                .map(EmailVerificationCodeEntity::toDomainModel);
    }

    @Override
    public Optional<EmailVerificationCode> findByUserIdAndCodeAndIsUsedFalse(Long userId, String code) {
        return emailVerificationRepositoryJpa.findByUserIdAndCodeAndIsUsedFalse(userId, code)
                .map(EmailVerificationCodeEntity::toDomainModel);
    }

    @Override
    public List<EmailVerificationCode> findByUserId(Long userId) {
        return emailVerificationRepositoryJpa.findByUserId(userId).stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .toList();
    }

    @Override
    public List<EmailVerificationCode> findByExpiresAtBefore(java.time.LocalDateTime dateTime) {
        return emailVerificationRepositoryJpa.findByExpiresAtBefore(dateTime).stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndIsUsedFalse(Long userId) {
        return emailVerificationRepositoryJpa.existsByUserIdAndIsUsedFalse(userId);
    }

    @Override
    public int markAllAsUsedByUserId(Long userId) {
        return emailVerificationRepositoryJpa.markAllAsUsedByUserId(userId);
    }

    @Override
    public int incrementAttempts(Long id) {
        return emailVerificationRepositoryJpa.incrementAttempts(id);
    }

    @Override
    public int deleteExpiredCodes(java.time.LocalDateTime dateTime) {
        return emailVerificationRepositoryJpa.deleteExpiredCodes(dateTime);
    }

    @Override
    public void markAsUsed(Long id) {
        emailVerificationRepositoryJpa.markAsUsed(id);
    }
}