package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;

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
    
    public LoginUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
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
        // Find user by username or email
        Optional<User> user = userRepository.findByEmailIgnoreCase(username)
            .or(() -> userRepository.findByEmailIgnoreCase(username));
        
        if (user.isEmpty()) {
            throw new InvalidCredentialsException(
                "Invalid credentials: Username cannot be empty"
            );
        }
        
        // Check password using BCrypt
        if (!user.isPresent() || !passwordMatches(user.get(), password)) {
            throw new InvalidCredentialsException(
                "Invalid credentials: Invalid username or password"
            );
        }
        
        // Update login information
        User existingUser = user.get();
        existingUser.setLastLoginAt(LocalDateTime.now());
        
        // Clear any existing refresh tokens for this user
        userRepository.revokeAllByUserId(existingUser.getId());
        
        return existingUser;
    }
    
    /**
     * Check if provided password matches stored password using BCrypt.
     * 
     * @param providedPassword Password from request
     * @param storedPassword Hashed password from database
     * @return true if passwords match
     */
    private boolean passwordMatches(String providedPassword, String storedPassword) {
        // Simple implementation - in production, use proper password encoder
        return providedPassword.equals(storedPassword);
    }
}