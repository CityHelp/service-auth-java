package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;

import java.time.LocalDateTime;

/**
 * Use case for user registration in CityHelp Auth Service.
 * Follows English naming convention for technical code.
 * 
 * Business Rules:
 * - Email must be unique and valid format
 * - Password must be at least 8 characters
 * - New users start with PENDING_VERIFICATION status
 * - Google OAuth2 users start as ACTIVE (pre-verified)
 * - Throw exception if user already exists
 * - Return user entity with generated UUID and timestamps
 */
@Service
public class RegisterUserUseCase {
    
    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    
    public RegisterUserUseCase(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
    }
    
    /**
     * Register a new user with email verification.
     * 
     * @param user User entity to register
     * @return User entity with generated UUID and timestamps
     * @throws UserAlreadyExistsException if user already exists
     */
    public User execute(User user) {
        // Check if user already exists by email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException(
                "User with email '" + user.getEmail() + "' already exists"
            );
        }
        
        // Set default values for a local user
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setRole(UserRole.USER);
        user.setIsVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        
        // Save user to repository
        User savedUser = userRepository.save(user);
        
        // Create email verification code
        String verificationCode = generateVerificationCode();
        
        // Create and save email verification code
        emailVerificationRepository.save(
            new com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode(
                    savedUser, verificationCode, LocalDateTime.now().plusMinutes(15)
            )
        );
        
        // For OAuth2 users, password should be null
        // For local users, use the provided password
        String password = (user.getOAuthProvider() == OAuthProvider.LOCAL) ? savedUser.getPassword() : null;

        return new User(
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                password,
                UserStatus.PENDING_VERIFICATION,
                UserRole.USER,
                savedUser.getCreatedAt()
            );
    }
    
    /**
     * Generate a 6-digit verification code.
     * 
     * @return 6-character alphanumeric string
     */
    private String generateVerificationCode() {
        return String.valueOf((int) (Math.random() * 1000000))
                .substring(0, 6);
    }
    
    /**
     * Get current user by their UUID.
     * 
     * @param uuid User unique identifier
     * @return Optional containing User if found, empty otherwise
     */
    public Optional<User> findByUuid(UUID uuid) {
        return userRepository.findByUuid(uuid);
    }
    
    /**
     * Check if a user needs email verification.
     * 
     * @param userId User ID to check
     * @return true if user is pending verification and has no active verification codes
     */
    public boolean needsEmailVerification(Long userId) {
        return userRepository.findById(userId)
                .map(user -> user.isPendingVerification() && emailVerificationRepository.findLatestByUserId(userId).isEmpty())
                .orElse(false));
    }
}