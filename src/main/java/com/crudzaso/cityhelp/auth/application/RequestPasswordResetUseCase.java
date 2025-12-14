package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import com.crudzaso.cityhelp.auth.application.service.EmailService;
import com.crudzaso.cityhelp.auth.application.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
public class RequestPasswordResetUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final int passwordResetExpirationHours;

    public RequestPasswordResetUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            EmailService emailService,
            @Value("${cityhelp.password-reset.expiration-hours:4}") int passwordResetExpirationHours
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordResetExpirationHours = passwordResetExpirationHours;
    }

    /**
     * Request password reset for a user.
     * Generates a reset token and sends it via email.
     *
     * @param email User's email
     * @throws UserNotFoundException if user not found
     */
    public void execute(String email) {
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            // For security, don't reveal if email exists or not
            // Just return silently
            return;
        }

        User user = userOpt.get();

        // Generate reset token with UTC timezone (configurable expiration)
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(passwordResetExpirationHours);

        PasswordResetToken resetToken = new PasswordResetToken(user.getId(), token, expiresAt);
        tokenRepository.save(resetToken);

        // Send email with reset token
        String fullName = user.getFirstName() + " " + user.getLastName();
        emailService.sendPasswordResetEmail(user.getEmail(), fullName, token);
    }
}
