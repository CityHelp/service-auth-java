package com.crudzaso.cityhelp.auth.domain.repository;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for User domain entity.
 * Follows English naming convention for technical code.
 * Pure Java interface without framework dependencies.
 * 
 * Contract for user data operations in the domain layer.
 * Infrastructure layer will provide the concrete implementation.
 */
public interface UserRepository {
    
    /**
     * Save a new user to the repository.
     * 
     * @param user the user entity to save
     * @return the saved user with generated ID
     */
    User save(User user);
    
    /**
     * Find a user by their unique UUID.
     * 
     * @param uuid the unique identifier of the user
     * @return Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUuid(UUID uuid);
    
    /**
     * Find a User by their email address.
     * 
     * @param email the email address to search for
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Find a User by their email address (case insensitive).
     * 
     * @param email the email address to search for
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmailIgnoreCase(String email);
    
    /**
     * Check if a user exists with the given email.
     * 
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if a User exists with the given email (case insensitive).
     * 
     * @param email the email address to check
     * @return true if a User with this email exists, false otherwise
     */
    boolean existsByEmailIgnoreCase(String email);
    
    /**
     * Find a User by their ID.
     * 
     * @param id the database ID of the user
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findById(Long id);
    
    /**
     * Find all users with a specific role.
     * 
     * @param role the role to filter by
     * @return list of users with the specified role
     */
    List<User> findByRole(UserRole role);
    
    /**
     * Find all users with a specific status.
     * 
     * @param status the status to filter by
     * @return list of users with the specified status
     */
    List<User> findByStatus(UserStatus status);
    
    /**
     * Find users by OAuth provider.
     * 
     * @param provider the OAuth provider to filter by
     * @return list of users with the specified OAuth provider
     */
    List<User> findByOAuthProvider(OAuthProvider provider);
    
    /**
     * Find users pending email verification.
     * 
     * @return list of users who need to verify their email
     */
    List<User> findByPendingVerification();
    
    /**
     * Find all active users.
     * 
     * @return list of active users
     */
    List<User> findActiveUsers();
    
    /**
     * Update a user entity.
     * 
     * @param user the user entity to update
     * @return the updated User Entity
     */
    User update(User user);
    
    /**
     * Delete a User by their ID.
     * 
     * @param id the database ID of the User to delete
     */
    void deleteById(Long id);
    
    /**
     * Delete a User by their UUID.
     * 
     * @param uuid the unique identifier of the User to delete
     */
    void deleteByUuid(UUID uuid);
    
    /**
     * Count total number of users.
     * 
     * @return total number of users in the system
     */
    long count();
    
    /**
     * Count users by status.
     * 
     * @param status the status to count
     * @return number of users with the specified status
     */
    long countByStatus(UserStatus status);
    
    /**
     * Count users by role.
     * 
     * @param role the role to count
     * @return number of users with the specified role
     */
    long countByRole(UserRole role);
    
    /**
     * Update the last login timestamp for a user.
     * 
     * @param userId the ID of the User to update
     */
    void updateLastLoginAt(Long userId);
    
    /**
     * Mark a User as verified.
     * 
     * @param userId the ID of the User to mark as verified
     */
    void markAsVerified(Long userId);
    
    /**
     * Change the status of a User.
     * 
     * @param userId the ID of the User to update
     * @param newStatus the new status to set
     */
    void updateStatus(Long userId, UserStatus newStatus);
}