package com.crudzaso.cityhelp.auth.domain.model;

import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * User domain entity for CityHelp authentication system.
 * Follows English naming convention for technical code.
 * Pure Java without framework dependencies.
 * 
 * Business Rules:
 * - Email must be unique and valid format
 * - Password must be at least 8 characters for LOCAL users
 * - OAuth users don't need password verification
 * - All users start with PENDING_VERIFICATION status
 * - Google OAuth users start as ACTIVE
 */
public class User {
    
    private Long id;
    private UUID uuid;
    private String firstName;
    private String lastName;
    private String email;
    private String password; // null for OAuth2 users
    private OAuthProvider oauthProvider;
    private Boolean isVerified;
    private UserStatus status;
    private UserRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastFailedLoginAttempt;

    // Default constructor
    public User() {}
    
    // Constructor for LOCAL registration
    public User(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.oauthProvider = OAuthProvider.LOCAL;
        this.status = UserStatus.PENDING_VERIFICATION;
        this.role = UserRole.USER;
        this.isVerified = false;
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
    
    // Constructor for OAuth2 users (Google)
    public User(String firstName, String lastName, String email, OAuthProvider provider) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.oauthProvider = provider;
        this.status = UserStatus.ACTIVE; // OAuth2 users are pre-verified
        this.role = UserRole.USER;
        this.isVerified = true;
        this.password = null; // OAuth2 users don't have password
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public OAuthProvider getOAuthProvider() { return oauthProvider; }
    public void setOAuthProvider(OAuthProvider oauthProvider) { this.oauthProvider = oauthProvider; }
    
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getLastFailedLoginAttempt() { return lastFailedLoginAttempt; }
    public void setLastFailedLoginAttempt(LocalDateTime lastFailedLoginAttempt) { this.lastFailedLoginAttempt = lastFailedLoginAttempt; }

    // Business logic methods (pure Java)
    public String getFullName() {
        
        return firstName + " " + lastName;
    }
    
    public boolean isLocalUser() {
        return OAuthProvider.LOCAL.equals(oauthProvider);
    }
    
    public boolean isOAuthUser() {
        return !OAuthProvider.LOCAL.equals(oauthProvider);
    }
    
    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }
    
    public boolean isPendingVerification() {
        return UserStatus.PENDING_VERIFICATION.equals(status);
    }
    
    public boolean canLogin() {
        return UserStatus.ACTIVE.equals(status) && isVerified && !isLocked();
    }

    public boolean isLocked() {
        if (lockedUntil == null) {
            return false;
        }
        return LocalDateTime.now(ZoneOffset.UTC).isBefore(lockedUntil);
    }

    public boolean needsEmailVerification() {
        return isLocalUser() && !isVerified;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "uuid='" + uuid + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status + '\'' +
                ", role=" + role + '\'' +
                '}';
    }
}