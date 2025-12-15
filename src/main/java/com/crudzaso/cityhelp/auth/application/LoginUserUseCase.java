package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Use case for traditional user login in CityHelp Auth Service.
 * Follows English naming convention for technical code.
 *
 * Business Rules:
 * - Rate limiting: Implement failed login attempt counting
 * - Password verification: Use BCrypt for password comparison
 * - Account lockout: Lock account after too many failed attempts
 * - Session management: Clear sessions on logout
 * - Token management: Handle refresh token lifecycle
 * - OAuth2 handling: Support social login with Google
 *
 * @param username Username or email
 * @param password User password
 * @return User entity with authentication tokens on successful login
 * @throws InvalidCredentialsException if credentials are invalid
 */
@Service
public class LoginUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    // Account lockout configuration
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    public LoginUserUseCase(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user with traditional credentials.
     * Implements account lockout after MAX_FAILED_ATTEMPTS failed login attempts.
     *
     * @param username Username or email
     * @param password User password
     * @return User entity with authentication tokens on successful login
     * @throws InvalidCredentialsException if credentials are invalid or account is locked
     */
    public User execute(String username, String password) {
        // Find user by email (case-insensitive)
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(username);

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = userOpt.get();

        // Check if account is locked
        if (user.isLocked()) {
            throw new InvalidCredentialsException(
                "Cuenta bloqueada temporalmente. Intente más tarde."
            );
        }

        // Verify user account status
        if (!user.canLogin()) {
            throw new InvalidCredentialsException(
                "Account is not active or not verified"
            );
        }

        // Check password using BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            // Handle failed login attempt
            handleFailedLoginAttempt(user);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Successful login - reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastFailedLoginAttempt(null);
        user.setLastLoginAt(LocalDateTime.now());
        User updatedUser = userRepository.update(user);

        // Revoke all existing refresh tokens for this user (new login invalidates old sessions)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return updatedUser;
    }

    /**
     * Handle failed login attempt - increment counter and lock if necessary.
     */
    private void handleFailedLoginAttempt(User user) {
        user.setLastFailedLoginAttempt(LocalDateTime.now());

        Integer attempts = user.getFailedLoginAttempts();
        if (attempts == null) {
            attempts = 0;
        }

        attempts++;
        user.setFailedLoginAttempts(attempts);

        // Lock account if max attempts reached
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
        }

        // Update user in database
        userRepository.update(user);
    }
}