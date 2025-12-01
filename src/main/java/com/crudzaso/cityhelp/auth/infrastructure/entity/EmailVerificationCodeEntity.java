package com.crudzaso.cityhelp.auth.infrastructure.entity;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * EmailVerificationCode entity implementation with JPA annotations.
 * Infrastructure layer implementation of EmailVerificationCode domain model.
 * Maps to 'email_verification_codes' table in PostgreSQL database.
 */
@Entity
@Table(name = "email_verification_codes")
public class EmailVerificationCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code", nullable = false, length = 6)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    // Default constructor required by JPA
    public EmailVerificationCodeEntity() {}

    // Constructor from domain model
    public EmailVerificationCodeEntity(EmailVerificationCode emailCode) {
        this.id = emailCode.getId();
        this.userId = emailCode.getUserId();
        this.code = emailCode.getCode();
        this.expiresAt = emailCode.getExpiresAt();
        this.createdAt = emailCode.getCreatedAt();
        this.isUsed = emailCode.isUsed();
        this.attempts = emailCode.getAttempts();
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
    public void setUsed(boolean used) { isUsed = used; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Convert to domain model (pure Java).
     *
     * @return EmailVerificationCode domain model
     */
    public EmailVerificationCode toDomainModel() {
        EmailVerificationCode emailCode = new EmailVerificationCode();
        emailCode.setId(id);
        emailCode.setUserId(userId);
        emailCode.setCode(code);
        emailCode.setExpiresAt(expiresAt);
        emailCode.setCreatedAt(createdAt);
        emailCode.setUsed(isUsed);
        emailCode.setAttempts(attempts);
        return emailCode;
    }

    @Override
    public String toString() {
        return "EmailVerificationCodeEntity{" +
                "id=" + id +
                ", userId=" + userId +
                ", code='" + code.substring(0, 2) + "****" + (code != null && code.length() > 4 ? code.substring(4) : "") + '\'' +
                ", expiresAt=" + expiresAt +
                ", isUsed=" + isUsed +
                ", attempts=" + attempts +
                '}';
    }
}