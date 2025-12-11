package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class ValidateResetTokenUseCase {

    private final PasswordResetTokenRepository tokenRepository;

    public ValidateResetTokenUseCase(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /**
     * Validate if a reset token is valid.
     *
     * @param token Reset token
     * @return true if token is valid, false otherwise
     */
    public boolean execute(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        return resetToken.isValid();
    }
}
