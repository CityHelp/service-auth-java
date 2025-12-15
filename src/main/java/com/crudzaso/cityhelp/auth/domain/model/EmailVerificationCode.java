package com.crudzaso.cityhelp.auth.domain.model;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * EmailVerificationCode domain entity for CityHelp authentication system.
 * Follows English naming convention for technical code.
 * Pure Java without framework dependencies.
 * 
 * Business Rules:
 * - Codes are 6 digits generated randomly
 * - Codes expire after 15 minutes
 * - Only one active code per user
 * - Codes are single-use (voided after verification)
 * - Cannot be reused once verified
 */
public class EmailVerificationCode {
    
    private Long id;
    private Long userId;
    private String code;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean isUsed;
    private int attempts;
    
    // Default constructor
    public EmailVerificationCode() {}
    
    // Constructor for code generation
    public EmailVerificationCode(Long userId, String code, LocalDateTime expiresAt) {
        this.userId = userId;
        this.code = code;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
        this.isUsed = false;
        this.attempts = 0;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public boolean isUsed() { return isUsed; }
    public void setUsed(boolean used) { this.isUsed = used; }
    
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    
    // Business logic methods (pure Java)
    public boolean isValid() {
        return !isUsed && !isExpired() && attempts < 3;
    }
    
    public boolean isExpired() {
        return LocalDateTime.now(ZoneOffset.UTC).isAfter(expiresAt);
    }

    public boolean willExpireInMinutes(int minutes) {
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(minutes).isAfter(expiresAt);
    }

    public long getRemainingMinutes() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(ZoneOffset.UTC), expiresAt).toMinutes();
    }
    
    public void markAsUsed() {
        this.isUsed = true;
    }
    
    public void incrementAttempts() {
        this.attempts++;
    }
    
    public boolean hasExceededAttempts() {
        return attempts >= 3;
    }
    
    public String maskCode() {
        if (code == null || code.length() != 6) {
            return "******";
        }
        return code.substring(0, 2) + "****" + code.substring(4);
    }
    
    @Override
    public String toString() {
        return "EmailVerificationCode{" +
                "id=" + id + '\'' +
                ", userId=" + userId + '\'' +
                ", code='" + maskCode() + '\'' +
                ", expiresAt=" + expiresAt + '\'' +
                ", isUsed=" + isUsed + '\'' +
                ", attempts=" + attempts + '\'' +
                '}';
    }
}