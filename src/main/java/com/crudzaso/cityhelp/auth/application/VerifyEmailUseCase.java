package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.application.service.EmailService;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for email verification in CityHelp Auth Service.
 * Follows English naming convention for technical code.
 *
 * Business Rules:
 * - Code must be 6 digits and single-use
 * - Code expires after 15 minutes
 * - Only one active code per user
 * - Cannot reuse codes
 * - Mark code as used after successful verification
 * - Delete expired codes
 *
 * @param userId User ID to generate code for
 * @param code 6-digit verification code
 * @return true if verification successful, false otherwise
 * @throws InvalidVerificationCodeException if code is invalid or expired
 */
@Slf4j
@Service
public class VerifyEmailUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

    public VerifyEmailUseCase(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailService = emailService;
    }

    /**
     * Verify email verification code for a user.
     *
     * @param userId User ID to verify code for
     * @param code 6-digit verification code
     * @return true if verification successful, false otherwise
     * @throws InvalidVerificationCodeException if code is invalid or expired
     */
    public boolean execute(Long userId, String code) {
        // Find user by ID
        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()) {
            throw new InvalidVerificationCodeException(
                "User not found for ID: " + userId
            );
        }

        // Find latest verification code for this user
        Optional<EmailVerificationCode> latestCode = emailVerificationRepository.findLatestByUserId(userId);

        // Validate code
        if (latestCode.isEmpty()) {
            throw new InvalidVerificationCodeException(
                "No verification code found for user ID: " + userId
            );
        }

        // Check if code has already been used
        if (latestCode.get().isUsed()) {
            throw new InvalidVerificationCodeException(
                "Verification code has already been used"
            );
        }

        // Check if code is valid and not expired
        if (!latestCode.get().isValid() ||
                LocalDateTime.now().isAfter(latestCode.get().getExpiresAt())) {
            throw new InvalidVerificationCodeException(
                    "Verification code has expired or is invalid"
            );
        }

        // CRITICAL: Validate that the code matches the stored code
        if (!latestCode.get().getCode().equals(code)) {
            EmailVerificationCode codeObj = latestCode.get();
            codeObj.incrementAttempts();
            emailVerificationRepository.update(codeObj);

            if (codeObj.hasExceededAttempts()) {
                throw new InvalidVerificationCodeException("Maximum verification attempts exceeded");
            }

            throw new InvalidVerificationCodeException("Invalid verification code");
        }

        // Mark code as used and update user status
        emailVerificationRepository.markAsUsedById(latestCode.get().getId());
        userRepository.updateStatus(userId, UserStatus.ACTIVE);

        // Send welcome email after successful verification
        try {
            User verifiedUser = user.get();
            String fullName = verifiedUser.getFirstName() + " " + verifiedUser.getLastName();
            emailService.sendWelcomeEmail(verifiedUser.getEmail(), fullName);
            log.info("Welcome email sent successfully to: {}", verifiedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}. Verification completed but email not sent.",
                    user.get().getEmail(), e);
            // Continue execution - verification is successful
        }

        return true;
    }
}