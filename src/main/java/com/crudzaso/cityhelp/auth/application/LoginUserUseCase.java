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
     *
     * @param username Username or email
     * @param password User password
     * @return User entity with authentication tokens on successful login
     * @throws InvalidCredentialsException if credentials are invalid
     */
    public User execute(String username, String password) {
        // Find user by email (case-insensitive)
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(username);

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        User user = userOpt.get();

        // Verify user account status
        if (!user.canLogin()) {
            throw new InvalidCredentialsException(
                "Account is not active or not verified"
            );
        }

        // Check password using BCrypt
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        User updatedUser = userRepository.update(user);

        // Revoke all existing refresh tokens for this user (new login invalidates old sessions)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        return updatedUser;
    }
}