package com.crudzaso.cityhelp.auth.application;

import com.crudzaso.cityhelp.auth.application.exception.UserAlreadyExistsException;
import com.crudzaso.cityhelp.auth.application.service.EmailService;
import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
@Slf4j
@Service
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public RegisterUserUseCase(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
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

        // Hash password before saving (BCrypt)
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // Set default values for a local user
        user.setUuid(UUID.randomUUID());  // Generate UUID for new user
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
        EmailVerificationCode emailCode = new EmailVerificationCode();
        emailCode.setUserId(savedUser.getId());
        emailCode.setCode(verificationCode);
        emailCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        emailCode.setCreatedAt(LocalDateTime.now());
        emailCode.setUsed(false);
        emailCode.setAttempts(0);

        emailVerificationRepository.save(emailCode);

        // Send verification code email
        try {
            String fullName = savedUser.getFirstName() + " " + savedUser.getLastName();
            emailService.sendVerificationCode(
                    savedUser.getEmail(),
                    fullName,
                    verificationCode
            );
            log.info("Verification code email sent successfully to: {}", savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}. User registered but email not sent.",
                    savedUser.getEmail(), e);
            // Continue execution - user is already registered
        }

        // Return the saved user
        return savedUser;
    }
    
    /**
     * Generate a 6-digit verification code.
     * Ensures the code is always exactly 6 digits by padding with zeros if necessary.
     *
     * @return 6-digit numeric string
     */
    private String generateVerificationCode() {
        int code = (int) (Math.random() * 1000000);
        return String.format("%06d", code);
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
                .orElse(false);
    }
}