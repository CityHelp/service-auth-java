package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ResetPasswordUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ResetPasswordUseCase.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public ResetPasswordUseCase(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Reset user password with a valid reset token.
     *
     * @param token Reset token
     * @param newPassword New password
     * @throws InvalidTokenException if token is invalid or expired
     */
    public void execute(String token, String newPassword) {
        logger.info("Starting password reset process with token: {}", token);

        try {
            // Find token
            Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                logger.warn("Token not found in database: {}", token);
                throw new InvalidTokenException("Token no válido");
            }

            PasswordResetToken resetToken = tokenOpt.get();
            logger.info("Token found. Used: {}, ExpiresAt: {}", resetToken.getUsed(), resetToken.getExpiresAt());

            // Validate token is valid (not used and not expired)
            if (!resetToken.isValid()) {
                logger.warn("Token invalid. Used: {}, Expired: {}", resetToken.getUsed(), resetToken.isExpired());
                throw new InvalidTokenException("Token expirado o ya utilizado");
            }

            // Find user
            Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
            if (userOpt.isEmpty()) {
                logger.warn("User not found for token. UserId: {}", resetToken.getUserId());
                throw new InvalidTokenException("Usuario no encontrado");
            }

            // Update password
            User user = userOpt.get();
            logger.info("Updating password for user ID: {}, email: {}", user.getId(), user.getEmail());
            String encodedPassword = passwordEncoder.encode(newPassword);
            logger.debug("Password encoded successfully");
            user.setPassword(encodedPassword);
            User updatedUser = userRepository.update(user);
            logger.info("Password updated successfully for user: {}", updatedUser.getEmail());

            // Mark token as used
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
            logger.info("Token marked as used");
        } catch (InvalidTokenException e) {
            logger.warn("Invalid token exception: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error in password reset: ", e);
            logger.error("Error class: {}, message: {}", e.getClass().getName(), e.getMessage());
            throw new RuntimeException("Error al restablecer contraseña: " + e.getMessage(), e);
        }
    }
}
