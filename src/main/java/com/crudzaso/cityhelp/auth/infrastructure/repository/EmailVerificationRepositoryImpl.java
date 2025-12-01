package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
<<<<<<< HEAD
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
=======
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

    List<EmailVerificationCodeEntity> findByExpiresAtBefore(LocalDateTime dateTime);

    boolean existsByUserIdAndIsUsedFalse(Long userId);

    @Modifying
    @Query("UPDATE EmailVerificationCodeEntity evc SET evc.isUsed = true WHERE evc.userId = :userId")
    int markAllAsUsedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE EmailVerificationCodeEntity evc SET evc.attempts = evc.attempts + 1 WHERE evc.id = :id")
    int incrementAttempts(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM EmailVerificationCodeEntity evc WHERE evc.expiresAt < :dateTime")
    int deleteExpiredCodes(@Param("dateTime") LocalDateTime dateTime);
}

/**
 * Repository implementation for EmailVerificationCode domain entity.
>>>>>>> feature/project_initiation
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
<<<<<<< HEAD
    public Optional<EmailVerificationCode> findByUserIdAndCodeAndIsUsedFalse(Long userId, String code) {
=======
    public Optional<EmailVerificationCode> findByUserIdAndCode(Long userId, String code) {
>>>>>>> feature/project_initiation
        return emailVerificationRepositoryJpa.findByUserIdAndCodeAndIsUsedFalse(userId, code)
                .map(EmailVerificationCodeEntity::toDomainModel);
    }

    @Override
<<<<<<< HEAD
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
=======
    public List<EmailVerificationCode> findAllByUserId(Long userId) {
        return emailVerificationRepositoryJpa.findByUserId(userId).stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailVerificationCode> findExpiredCodes() {
        return emailVerificationRepositoryJpa.findByExpiresAtBefore(LocalDateTime.now()).stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailVerificationCode> findAll() {
        return emailVerificationRepositoryJpa.findAll().stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        emailVerificationRepositoryJpa.deleteById(id);
>>>>>>> feature/project_initiation
    }

    @Override
    public void markAsUsed(Long id) {
<<<<<<< HEAD
        emailVerificationRepositoryJpa.markAsUsed(id);
=======
        emailVerificationRepositoryJpa.findById(id).ifPresent(entity -> {
            entity.setUsed(true);
            emailVerificationRepositoryJpa.save(entity);
        });
    }

    @Override
    public void markAllAsUsedByUserId(Long userId) {
        emailVerificationRepositoryJpa.markAllAsUsedByUserId(userId);
    }

    @Override
    public void incrementAttempts(Long id) {
        emailVerificationRepositoryJpa.incrementAttempts(id);
    }

    @Override
    public void deleteExpiredCodes() {
        emailVerificationRepositoryJpa.deleteExpiredCodes(LocalDateTime.now());
    }

    @Override
    public void deleteAllByUserId(Long userId) {
        emailVerificationRepositoryJpa.findByUserId(userId)
                .forEach(emailVerificationRepositoryJpa::delete);
    }

    @Override
    public long count() {
        return emailVerificationRepositoryJpa.count();
    }

    @Override
    public long countByUserId(Long userId) {
        return emailVerificationRepositoryJpa.findByUserId(userId).size();
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return emailVerificationRepositoryJpa.existsByUserIdAndIsUsedFalse(userId);
>>>>>>> feature/project_initiation
    }
}