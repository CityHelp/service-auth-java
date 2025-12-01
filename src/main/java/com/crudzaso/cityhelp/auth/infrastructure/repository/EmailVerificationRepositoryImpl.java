package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.infrastructure.entity.EmailVerificationCodeEntity;
import com.crudzaso.cityhelp.auth.infrastructure.repository.jpa.EmailVerificationRepositoryJpa;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Infrastructure implementation of EmailVerificationRepository interface.
 * Implements domain repository contract using JPA.
 *
 * Business Rules:
 * - Convert between JPA entities and domain models
 * - Handle database operations with proper error handling
 * - Maintain clean architecture separation
 */
@Repository
public class EmailVerificationRepositoryImpl implements EmailVerificationRepository {

    private final EmailVerificationRepositoryJpa jpaRepository;

    public EmailVerificationRepositoryImpl(EmailVerificationRepositoryJpa jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EmailVerificationCode save(EmailVerificationCode emailVerificationCode) {
        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity(emailVerificationCode);
        EmailVerificationCodeEntity savedEntity = jpaRepository.save(entity);
        return savedEntity.toDomainModel();
    }

    @Override
    public Optional<EmailVerificationCode> findLatestByUserId(Long userId) {
        return jpaRepository.findByUserIdAndIsUsedFalse(userId)
                .map(EmailVerificationCodeEntity::toDomainModel);
    }

    @Override
    public Optional<EmailVerificationCode> findByUserIdAndCode(Long userId, String code) {
        return jpaRepository.findByUserIdAndCodeAndIsUsedFalse(userId, code)
                .map(EmailVerificationCodeEntity::toDomainModel);
    }

    @Override
    public List<EmailVerificationCode> findByUserId(Long userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public EmailVerificationCode update(EmailVerificationCode verificationCode) {
        EmailVerificationCodeEntity entity = new EmailVerificationCodeEntity(verificationCode);
        EmailVerificationCodeEntity updatedEntity = jpaRepository.save(entity);
        return updatedEntity.toDomainModel();
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
    public int deleteExpired() {
        return jpaRepository.deleteExpired(LocalDateTime.now());
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<EmailVerificationCode> findActive() {
        return jpaRepository.findByIsUsedFalse().stream()
                .filter(entity -> entity.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailVerificationCode> findExpired() {
        return jpaRepository.findByExpiresAtBefore(LocalDateTime.now()).stream()
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<EmailVerificationCode> findUsed() {
        return jpaRepository.findAll().stream()
                .filter(EmailVerificationCodeEntity::isUsed)
                .map(EmailVerificationCodeEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public int deleteUsed() {
        List<EmailVerificationCodeEntity> usedCodes = jpaRepository.findAll().stream()
                .filter(EmailVerificationCodeEntity::isUsed)
                .collect(Collectors.toList());

        jpaRepository.deleteAll(usedCodes);
        return usedCodes.size();
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
        return jpaRepository.countUnused();
    }

    @Override
    public long countExpired() {
        return jpaRepository.countExpired(LocalDateTime.now());
    }
}
