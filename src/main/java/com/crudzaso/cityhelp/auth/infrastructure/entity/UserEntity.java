package com.crudzaso.cityhelp.auth.infrastructure.entity;

import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.model.User;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity implementation with JPA annotations.
 * Infrastructure layer implementation of the User domain model.
 * Maps to the 'users' table in PostgreSQL database.
 *
 * Extends AuditableEntity to automatically manage createdAt and updatedAt timestamps.
 */
@Entity
@Table(name = "users")
public class UserEntity extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private UUID uuid;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password", length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    // Default constructor required by JPA
    public UserEntity() {}

    // Constructor from domain model
    public UserEntity(User user) {
        this.id = user.getId();
        this.uuid = user.getUuid();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.oauthProvider = user.getOAuthProvider();
        this.isVerified = user.getIsVerified();
        this.status = user.getStatus();
        this.role = user.getRole();
        this.setCreatedAt(user.getCreatedAt());
        this.setUpdatedAt(user.getUpdatedAt());
        this.lastLoginAt = user.getLastLoginAt();
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

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    /**
     * Convert to domain model (pure Java).
     *
     * @return User domain model
     */
    public User toDomainModel() {
        User user = new User();
        user.setId(id);
        user.setUuid(uuid);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPassword(password);
        user.setOAuthProvider(oauthProvider);
        user.setIsVerified(isVerified);
        user.setStatus(status);
        user.setRole(role);
        user.setCreatedAt(getCreatedAt());
        user.setUpdatedAt(getUpdatedAt());
        user.setLastLoginAt(lastLoginAt);
        return user;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", role=" + role +
                '}';
    }
}