package com.crudzaso.cityhelp.auth.infrastructure.entity;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * RefreshToken entity implementation with JPA annotations.
 * Infrastructure layer implementation of RefreshToken domain model.
 * Maps to 'refresh_tokens' table in PostgreSQL database.
 *
 * Note: RefreshTokens don't need updatedAt as they are immutable once created.
 * Manages createdAt timestamp directly without extending AuditableEntity.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked;

    // Default constructor required by JPA
    public RefreshTokenEntity() {}

    // Constructor from domain model
    public RefreshTokenEntity(RefreshToken refreshToken) {
        this.id = refreshToken.getId();
        this.token = refreshToken.getToken();
        this.userId = refreshToken.getUserId();
        this.expiresAt = refreshToken.getExpiresAt();
        this.createdAt = refreshToken.getCreatedAt();
        this.isRevoked = refreshToken.isRevoked();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isRevoked() { return isRevoked; }
    public void setRevoked(boolean revoked) { isRevoked = revoked; }

    /**
     * Convert to domain model (pure Java).
     *
     * @return RefreshToken domain model
     */
    public RefreshToken toDomainModel() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(id);
        refreshToken.setToken(token);
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setCreatedAt(getCreatedAt());
        refreshToken.setRevoked(isRevoked);
        return refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", expiresAt=" + expiresAt +
                ", isRevoked=" + isRevoked +
                '}';
    }
}