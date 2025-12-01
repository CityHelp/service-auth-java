package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Use case for JWT token refresh in CityHelp Auth Service.
 * Follows English naming convention for technical code.
 *
 * Business Rules:
 * - Validate refresh token exists and is not expired
 * - Validate refresh token is not revoked
 * - Revoke old refresh token after successful refresh
 * - Return user associated with refresh token
 *
 * @param refreshToken Current refresh token to validate
 * @return User entity if token is valid
 * @throws InvalidCredentialsException if token is invalid, expired, or revoked
 */
@Service
public class RefreshTokenUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Refresh JWT token for authenticated user.
     *
     * @param tokenValue Current refresh token to validate
     * @return User entity if token is valid
     * @throws InvalidCredentialsException if token is invalid
     */
    public User execute(String tokenValue) {
        // Find refresh token by value
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(tokenValue);

        if (tokenOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        RefreshToken token = tokenOpt.get();

        // Check if token is expired
        if (token.isExpired()) {
            throw new InvalidCredentialsException("Refresh token has expired");
        }

        // Check if token is revoked
        if (token.isRevoked()) {
            throw new InvalidCredentialsException("Refresh token has been revoked");
        }

        // Find user associated with token
        Optional<User> userOpt = userRepository.findById(token.getUserId());

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("User not found");
        }

        User user = userOpt.get();

        // Verify user can login (active and verified)
        if (!user.canLogin()) {
            throw new InvalidCredentialsException("User account is not active");
        }

        // Revoke old refresh token
        token.setRevoked(true);
        refreshTokenRepository.update(token);

        return user;
    }

    /**
     * Generate a new secure refresh token.
     * Uses UUID for cryptographically secure random token generation.
     *
     * @param userId User ID to associate token with
     * @param expirationDays Number of days until token expires (default: 7)
     * @return Newly created RefreshToken entity
     */
    public RefreshToken generateNewToken(Long userId, int expirationDays) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());

        return refreshTokenRepository.save(refreshToken);
    }
}
