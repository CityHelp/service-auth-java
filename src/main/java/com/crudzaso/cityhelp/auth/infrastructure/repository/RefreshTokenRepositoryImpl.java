package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
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
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA interface for RefreshToken operations.
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
}

/**
 * Repository implementation for RefreshToken domain entity.
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
    public void deleteById(Long id) {
        refreshTokenRepositoryJpa.deleteById(id);
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepositoryJpa.findByToken(token)
                .ifPresent(refreshTokenRepositoryJpa::delete);
    }

    @Override
    public void revokeAllTokensForUser(Long userId) {
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
    public void deleteExpiredTokens() {
        refreshTokenRepositoryJpa.deleteExpiredTokens(LocalDateTime.now());
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
    public boolean existsByToken(String token) {
        return refreshTokenRepositoryJpa.existsByToken(token);
    }
}