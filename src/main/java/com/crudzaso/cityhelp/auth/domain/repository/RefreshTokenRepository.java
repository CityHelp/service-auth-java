package com.crudzaso.cityhelp.auth.domain.repository;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for RefreshToken domain entity.
 * Follows English naming convention for technical code.
 * Pure Java interface without framework dependencies.
 * 
 * Contract for refresh token data operations in the domain layer.
 * Infrastructure layer will provide the concrete implementation.
 */
public interface RefreshTokenRepository {
    
    /**
     * Save a new refresh token to the repository.
     * 
     * @param refreshToken the refresh token entity to save
     * @return the saved refresh token with generated ID
     */
    RefreshToken save(RefreshToken refreshToken);
    
    /**
     * Find a refresh token by its token string.
     * 
     * @param token the token string to search for
     * @return Optional containing the RefreshToken if found, empty otherwise
     */
    Optional<RefreshToken> findByToken(String token);
    
    /**
     * Find a refresh token by its database ID.
     * 
     * @param id the database ID of the refresh token
     * @return Optional containing the RefreshToken if found, empty otherwise
     */
    Optional<RefreshToken> findById(Long id);
    
    /**
     * Find all refresh tokens for a specific user.
     * 
     * @param userId the ID of the User to find tokens for
     * @return list of refresh tokens belonging to the user
     */
    List<RefreshToken> findByUserId(Long userId);
    
    /**
     * Find all active (non-revoked) refresh tokens for a user.
     * 
     * @param userId the ID of the User to find active tokens for
     * @return list of active refresh tokens for the User
     */
    List<RefreshToken> findActiveByUserId(Long userId);
    
    /**
     * Find all expired refresh tokens.
     * 
     * @return list of expired refresh tokens
     */
    List<RefreshToken> findExpired();
    
    /**
     * Find all revoked refresh tokens.
     * 
     * @return list of revoked refresh tokens
     */
    List<RefreshToken> findRevoked();
    
    /**
     * Revoke (mark as invalid) all refresh tokens for a user.
     * 
     * @param userId the ID of the User to revoke tokens for
     */
    void revokeAllByUserId(Long userId);
    
    /**
     * Revoke a specific refresh token.
     * 
     * @param tokenId the ID of the refresh token to revoke
     */
    void revokeById(Long tokenId);
    
    /**
     * Revoke a refresh token by its token string.
     * 
     * @param token the token string to revoke
     */
    void revokeByToken(String token);
    
    /**
     * Delete a refresh token by its ID.
     * 
     * @param id the database ID of the refresh token to delete
     */
    void deleteById(Long id);
    
    /**
     * Delete all refresh tokens for a specific user.
     * 
     * @param userId the ID of the User to delete tokens for
     */
    void deleteAllByUserId(Long userId);
    
    /**
     * Clean up expired refresh tokens.
     * 
     * @return number of tokens deleted
     */
    int deleteExpired();
    
    /**
     * Count active refresh tokens for a User.
     * 
     * @param userId the ID of the User to count tokens for
     * @return number of active refresh tokens for the User
     */
    long countActiveByUserId(Long userId);
    
    /**
     * Check if a User has any active refresh tokens.
     * 
     * @param userId the ID of the User to check
     * @return true if the User has at least one active refresh token
     */
    boolean hasActiveTokens(Long userId);
    
    /**
     * Update a refresh token entity.
     * 
     * @param refreshToken the refresh token entity to update
     * @return the updated RefreshToken Entity
     */
    RefreshToken update(RefreshToken refreshToken);
}