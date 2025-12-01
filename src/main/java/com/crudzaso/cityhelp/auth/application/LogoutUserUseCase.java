package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for user logout in CityHelp Auth Service.
 * Follows English naming convention for technical code.
 * 
 * Business Rules:
 * - Revoke all refresh tokens for user
 * - Clear authentication state
 * - Provide feedback for successful logout
 * 
 * @param userId User ID to logout
 * @return boolean indicating if logout was successful
 */
@Service
public class LogoutUserUseCase {
    
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    
    public LogoutUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }
    
    /**
     * Logout user and invalidate all their refresh tokens.
     *
     * @param userId User ID to logout
     * @return true if logout was successful, false otherwise
     */
    public boolean execute(Long userId) {
        // Find user by ID
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            return false;
        }

        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllByUserId(userId);

        return true;
    }
}