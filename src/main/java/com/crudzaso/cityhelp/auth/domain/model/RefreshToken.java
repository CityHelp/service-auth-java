package com.crudzaso.cityhelp.auth.domain.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * RefreshToken domain entity for CityHelp authentication system.
 * Follows English naming convention for technical code.
 * Pure Java without framework dependencies.
 * 
 * Business Rules:
 * - Each user can have multiple active refresh tokens
 * - Tokens must expire after 7 days
 * - Tokens are revoked on logout or new token generation
 * - Tokens must be unique and secure
 */
public class RefreshToken {
    
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean isRevoked;
    
    // Default constructor
    public RefreshToken() {}
    
    // Constructor for token creation
    public RefreshToken(String token, Long userId, LocalDateTime expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        this.isRevoked = false;
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
    public void setRevoked(boolean revoked) { this.isRevoked = revoked; }
    
    // Business logic methods (pure Java)
    public boolean isValid() {
        return !isRevoked && LocalDateTime.now(ZoneOffset.UTC).isBefore(expiresAt);
    }

    public boolean isExpired() {
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(expiresAt);
    }

    public boolean willExpireInHours(int hours) {
        return LocalDateTime.now(ZoneOffset.UTC).plusHours(hours).isAfter(expiresAt);
    }

    public long getRemainingHours() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(ZoneOffset.UTC), expiresAt).toHours();
    }
    
    public void revoke() {
        this.isRevoked = true;
    }
    
    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id + '\'' +
                ", userId=" + userId + '\'' +
                ", expiresAt=" + expiresAt + '\'' +
                ", isRevoked=" + isRevoked + '\'' +
                '}';
    }
}