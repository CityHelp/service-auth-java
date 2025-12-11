package com.crudzaso.cityhelp.auth.domain.repository;

import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepository {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUserId(Long userId);

    void deleteExpiredTokens();
}
