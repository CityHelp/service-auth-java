package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.infrastructure.entity.RefreshTokenEntity;
import com.crudzaso.cityhelp.auth.infrastructure.repository.jpa.RefreshTokenRepositoryJpa;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository implementation for RefreshToken domain entity.
 * Infrastructure layer - Implementation of domain repository contract using JPA.
 */
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenRepositoryJpa refreshTokenRepositoryJpa;

    public RefreshTokenRepositoryImpl(RefreshTokenRepositoryJpa refreshTokenRepositoryJpa) {
        this.refreshTokenRepositoryJpa = refreshTokenRepositoryJpa;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity(refreshToken);
        RefreshTokenEntity savedEntity = refreshTokenRepositoryJpa.save(entity);
        return savedEntity.toDomainModel();
    }

    @Override
    public Optional<RefreshToken> findById(Long id) {
        return refreshTokenRepositoryJpa.findById(id)
                .map(RefreshTokenEntity::toDomainModel);
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepositoryJpa.findByToken(token)
                .map(RefreshTokenEntity::toDomainModel);
    }

    @Override
    public List<RefreshToken> findByUserId(Long userId) {
        return refreshTokenRepositoryJpa.findByUserId(userId).stream()
                .map(RefreshTokenEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<RefreshToken> findValidTokensByUserId(Long userId) {
        return refreshTokenRepositoryJpa.findByUserIdAndIsRevokedFalse(userId).stream()
                .filter(entity -> !entity.toDomainModel().isExpired())
                .map(RefreshTokenEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<RefreshToken> findExpiredTokens() {
        return refreshTokenRepositoryJpa.findByExpiresAtBefore(LocalDateTime.now()).stream()
                .map(RefreshTokenEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<RefreshToken> findAll() {
        return refreshTokenRepositoryJpa.findAll().stream()
                .map(RefreshTokenEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public RefreshToken update(RefreshToken refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity(refreshToken);
        RefreshTokenEntity updatedEntity = refreshTokenRepositoryJpa.save(entity);
        return updatedEntity.toDomainModel();
    }

    @Override
    public void deleteById(Long id) {
        refreshTokenRepositoryJpa.deleteById(id);
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepositoryJpa.findByToken(token)
                .ifPresent(refreshTokenRepositoryJpa::delete);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        refreshTokenRepositoryJpa.revokeAllTokensForUser(userId);
    }

    @Override
    public void revokeToken(String token) {
        refreshTokenRepositoryJpa.findByToken(token)
                .ifPresent(entity -> {
                    entity.setRevoked(true);
                    refreshTokenRepositoryJpa.save(entity);
                });
    }

    @Override
    public int deleteExpired() {
        return refreshTokenRepositoryJpa.deleteExpiredTokens(LocalDateTime.now());
    }

    @Override
    public long count() {
        return refreshTokenRepositoryJpa.count();
    }

    @Override
    public long countByUserId(Long userId) {
        return refreshTokenRepositoryJpa.findByUserId(userId).size();
    }

    @Override
    public long countActiveByUserId(Long userId) {
        return refreshTokenRepositoryJpa.countActiveByUserId(userId, LocalDateTime.now());
    }

    @Override
    public boolean existsByToken(String token) {
        return refreshTokenRepositoryJpa.existsByToken(token);
    }
}
