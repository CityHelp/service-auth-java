package com.crudzaso.cityhelp.auth.infrastructure.repository;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.infrastructure.entity.UserEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of UserRepository interface.
 * Infrastructure layer repository that implements domain contract.
 * Uses Spring Data JPA for database operations.
 */
@Repository
@Transactional
public interface UserRepositoryJpa extends JpaRepository<UserEntity, Long> {

    // Custom query methods following Spring Data naming conventions
    Optional<UserEntity> findByUuid(UUID uuid);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<UserEntity> findByRole(UserRole role);

    List<UserEntity> findByStatus(UserStatus status);

    List<UserEntity> findByOAuthProvider(OAuthProvider provider);

    List<UserEntity> findByStatusAndIsVerified(UserStatus status, boolean isVerified);

    List<UserEntity> findByStatusAndOAuthProvider(UserStatus status, OAuthProvider provider);

    // Update operations
    @Transactional
    int updateLastLoginAtById(Long userId, java.time.LocalDateTime lastLoginAt);

    @Transactional
    int updateIsVerifiedById(Long userId, boolean isVerified);

    @Transactional
    int updateStatusById(Long userId, UserStatus status);
}

/**
 * Repository implementation class that bridges domain and infrastructure.
 * This class implements the UserRepository domain interface.
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserRepositoryJpa userRepositoryJpa;

    public UserRepositoryImpl(UserRepositoryJpa userRepositoryJpa) {
        this.userRepositoryJpa = userRepositoryJpa;
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity(user);
        UserEntity savedEntity = userRepositoryJpa.save(entity);
        return savedEntity.toDomainModel();
    }

    @Override
    public Optional<User> findByUuid(UUID uuid) {
        return userRepositoryJpa.findByUuid(uuid)
                .map(UserEntity::toDomainModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepositoryJpa.findByEmail(email)
                .map(UserEntity::toDomainModel);
    }

    @Override
    public Optional<User> findByEmailIgnoreCase(String email) {
        return userRepositoryJpa.findByEmailIgnoreCase(email)
                .map(UserEntity::toDomainModel);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepositoryJpa.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return userRepositoryJpa.existsByEmailIgnoreCase(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepositoryJpa.findById(id)
                .map(UserEntity::toDomainModel);
    }

    @Override
    public List<User> findByRole(UserRole role) {
        return userRepositoryJpa.findByRole(role).stream()
                .map(UserEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByStatus(UserStatus status) {
        return userRepositoryJpa.findByStatus(status).stream()
                .map(UserEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByOAuthProvider(OAuthProvider provider) {
        return userRepositoryJpa.findByOAuthProvider(provider).stream()
                .map(UserEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByPendingVerification() {
        return userRepositoryJpa.findByStatusAndIsVerified(UserStatus.PENDING_VERIFICATION, false).stream()
                .map(UserEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findActiveUsers() {
        return userRepositoryJpa.findByStatus(UserStatus.ACTIVE).stream()
                .map(UserEntity::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public User update(User user) {
        UserEntity entity = new UserEntity(user);
        UserEntity updatedEntity = userRepositoryJpa.save(entity);
        return updatedEntity.toDomainModel();
    }

    @Override
    public void deleteById(Long id) {
        userRepositoryJpa.deleteById(id);
    }

    @Override
    public void deleteByUuid(UUID uuid) {
        userRepositoryJpa.findByUuid(uuid)
                .ifPresent(userRepositoryJpa::delete);
    }

    @Override
    public long count() {
        return userRepositoryJpa.count();
    }

    @Override
    public long countByStatus(UserStatus status) {
        return userRepositoryJpa.findByStatus(status).size();
    }

    @Override
    public long countByRole(UserRole role) {
        return userRepositoryJpa.findByRole(role).size();
    }

    @Override
    public void updateLastLoginAt(Long userId) {
        userRepositoryJpa.updateLastLoginAtById(userId, java.time.LocalDateTime.now());
    }

    @Override
    public void markAsVerified(Long userId) {
        userRepositoryJpa.updateIsVerifiedById(userId, true);
    }

    @Override
    public void updateStatus(Long userId, UserStatus newStatus) {
        userRepositoryJpa.updateStatusById(userId, newStatus);
    }
}