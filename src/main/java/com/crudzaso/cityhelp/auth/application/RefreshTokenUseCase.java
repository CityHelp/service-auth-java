package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for JWT token refresh in CityHelp Auth Service.
 * Follows English naming convention for technical code.
 * 
 * Business Rules:
 * - Rate limiting: Prevent abuse of refresh endpoint
 * - Token rotation: Generate new tokens with different expiration times
 * - Security: Validate old token before issuing new one
 * - Token validation: Ensure token format and expiration
 * - Session management: Invalidate old tokens after refresh
 * 
 * @param userId User ID requesting token refresh
 * @param refreshToken Current refresh token to validate
 * @return RefreshTokenResponse with new token or error message
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
     * @param userId User ID requesting token refresh
     * @param refreshToken Current refresh token to validate
     * @return RefreshTokenResponse with new token or error message
     */
    public RefreshTokenResponse execute(Long userId, String refreshToken) {
        // Find user by ID
        Optional<User> user = userRepository.findById(userId);
        
        if (user.isEmpty()) {
            return new RefreshTokenResponse(false, "User not found");
        }
        
        // Validate refresh token
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByToken(refreshToken);
        
        if (existingToken.isEmpty()) {
            return new RefreshTokenResponse(false, "Invalid refresh token");
        }
        
        if (!existingToken.get().isValid() || 
                LocalDateTime.now().isAfter(existingToken.getExpiresAt())) {
            return new RefreshTokenResponse(false, "Invalid or expired refresh token");
            }
        
        // Check rate limiting (basic implementation)
        // In production, implement proper rate limiting with Redis
        // For now, we'll just check if user has any active tokens
        if (userRepository.hasActiveTokens(user.getId())) {
            return new RefreshTokenResponse(false, "Maximum refresh tokens reached. Please try again later.");
            }
        
        // Revoke all existing refresh tokens for this user
        refreshTokenRepository.revokeAllByUserId(user.getId());
        
        // Generate new refresh token
        RefreshToken newToken = new RefreshToken();
        newToken.setToken(generateRefreshToken());
        newToken.setUser(user);
        newToken.setCreatedAt(LocalDateTime.now());
        
        // Save new token
        refreshTokenRepository.save(newToken);
        
        // Update user with new token
        user.setRefreshToken(newToken.getToken());
        
        return new RefreshTokenResponse(true, "Token refreshed successfully", newToken);
    }
}