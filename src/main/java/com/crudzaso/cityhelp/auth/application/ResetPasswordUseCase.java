package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ResetPasswordUseCase {

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
        // Find token
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new InvalidTokenException("Token no válido");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Validate token is valid (not used and not expired)
        if (!resetToken.isValid()) {
            throw new InvalidTokenException("Token expirado o ya utilizado");
        }

        // Find user
        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());
        if (userOpt.isEmpty()) {
            throw new InvalidTokenException("Usuario no encontrado");
        }

        // Update password
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.update(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
