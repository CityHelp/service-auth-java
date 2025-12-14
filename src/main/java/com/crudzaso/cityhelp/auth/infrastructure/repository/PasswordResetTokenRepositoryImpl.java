package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import com.crudzaso.cityhelp.auth.infrastructure.entity.PasswordResetTokenEntity;
import com.crudzaso.cityhelp.auth.infrastructure.repository.jpa.PasswordResetTokenRepositoryJpa;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenRepositoryJpa jpaRepository;

    public PasswordResetTokenRepositoryImpl(PasswordResetTokenRepositoryJpa jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenEntity entity;

        // If token has an ID, it's an update; otherwise, it's a new insert
        if (token.getId() != null) {
            entity = jpaRepository.findById(token.getId()).orElse(null);
            if (entity != null) {
                entity.setUsed(token.getUsed());
                entity.setCreatedAt(token.getCreatedAt());
            } else {
                // Fallback: create new entity if not found by ID
                entity = new PasswordResetTokenEntity(
                    token.getUserId(),
                    token.getToken(),
                    token.getExpiresAt()
                );
                entity.setUsed(token.getUsed());
                entity.setCreatedAt(token.getCreatedAt());
            }
        } else {
            // New entity
            entity = new PasswordResetTokenEntity(
                token.getUserId(),
                token.getToken(),
                token.getExpiresAt()
            );
            entity.setUsed(token.getUsed());
            entity.setCreatedAt(token.getCreatedAt());
        }

        PasswordResetTokenEntity saved = jpaRepository.save(entity);
        return toDomainModel(saved);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token)
                .map(this::toDomainModel);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public void deleteExpiredTokens() {
        jpaRepository.deleteExpiredTokens(LocalDateTime.now(ZoneOffset.UTC));
    }

    private PasswordResetToken toDomainModel(PasswordResetTokenEntity entity) {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(entity.getId());
        token.setUserId(entity.getUserId());
        token.setToken(entity.getToken());
        token.setExpiresAt(entity.getExpiresAt());
        token.setUsed(entity.getUsed());
        token.setCreatedAt(entity.getCreatedAt());
        return token;
    }
}
