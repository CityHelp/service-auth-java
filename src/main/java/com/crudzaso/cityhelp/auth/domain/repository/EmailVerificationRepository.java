package com.crudzaso.cityhelp.auth.domain.repository;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for EmailVerificationCode domain entity.
 * Follows English naming convention for technical code.
 * Pure Java interface without framework dependencies.
 * 
 * Contract for email verification code data operations in the domain layer.
 * Infrastructure layer will provide the concrete implementation.
 */
public interface EmailVerificationRepository {
    
    /**
     * Save a new email verification code to the repository.
     * 
     * @param verificationCode the email verification code entity to save
     * @return the saved email verification code with generated ID
     */
    EmailVerificationCode save(EmailVerificationCode verificationCode);
    
    /**
     * Find an email verification code by its database ID.
     * 
     * @param id the database ID of the email verification code
     * @return Optional containing the EmailVerificationCode if found, empty otherwise
     */
    Optional<EmailVerificationCode> findById(Long id);
    
    /**
     * Find the latest active email verification code for a User.
     * 
     * @param userId the ID of the User to find the verification code for
     * @return Optional containing the latest EmailVerificationCode if found, empty otherwise
     */
    Optional<EmailVerificationCode> findLatestByUserId(Long userId);
    
    /**
     * Find all email verification codes for a specific User.
     * 
     * @param userId the ID of the User to find verification codes for
     * @return list of email verification codes belonging to the User
     */
    List<EmailVerificationCode> findByUserId(Long userId);
    
    /**
     * Find all active (unused and not expired) email verification codes.
     * 
     * @return list of active email verification codes
     */
    List<EmailVerificationCode> findActive();
    
    /**
     * Find all expired email verification codes.
     * 
     * @return list of expired email verification codes
     */
    List<EmailVerificationCode> findExpired();
    
    /**
     * Find all used email verification codes.
     * 
     * @return list of used email verification codes
     */
    List<EmailVerificationCode> findUsed();
    
    /**
     * Mark an email verification code as used.
     * 
     * @param verificationCode the EmailVerificationCode to mark as used
     */
    void markAsUsed(EmailVerificationCode verificationCode);
    
    /**
     * Mark an email verification code as used by its ID.
     * 
     * @param id the ID of the EmailVerificationCode to mark as used
     */
    void markAsUsedById(Long id);
    
    /**
     * Delete an email verification code by its ID.
     * 
     * @param id the database ID of the EmailVerificationCode to delete
     */
    void deleteById(Long id);
    
    /**
     * Delete all email verification codes for a specific User.
     * 
     * @param userId the ID of the User to delete verification codes for
     */
    void deleteAllByUserId(Long userId);
    
    /**
     * Clean up all expired email verification codes.
     * 
     * @return number of codes deleted
     */
    int deleteExpired();
    
    /**
     * Delete all used email verification codes.
     * 
     * @return number of codes deleted
     */
    int deleteUsed();
    
    /**
     * Count active email verification codes for a User.
     * 
     * @param userId the ID of the User to count codes for
     * @return number of active email verification codes for the User
     */
    long countActiveByUserId(Long userId);
    
    /**
     * Check if a User has any active email verification codes.
     * 
     * @param userId the ID of the User to check
     * @return true if the User has at least one active email verification code
     */
    boolean hasActiveCodes(Long userId);
    
    /**
     * Update an email verification code entity.
     * 
     * @param verificationCode the EmailVerificationCode entity to update
     * @return the updated EmailVerificationCode Entity
     */
    EmailVerificationCode update(EmailVerificationCode verificationCode);
}