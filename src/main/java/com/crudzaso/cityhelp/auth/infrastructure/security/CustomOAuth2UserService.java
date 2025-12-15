package com.crudzaso.cityhelp.auth.infrastructure.security;

import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Custom OAuth2 User Service for Google authentication.
 * Handles user registration/update after successful Google OAuth2 login.
 *
 * Flow:
 * 1. Receive OAuth2UserRequest from Google
 * 2. Load user information from Google
 * 3. Extract email, firstName, lastName from Google profile
 * 4. Check if user exists by email
 * 5. If NOT exists: create new user with status=ACTIVE, oauthProvider=GOOGLE
 * 6. If exists AND is LOCAL: update to GOOGLE provider
 * 7. If exists AND is GOOGLE: update user information if changed
 * 8. Return OAuth2User
 *
 * @author CityHelp Team
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Load user from OAuth2 provider (Google) and create/update in our database.
     *
     * @param userRequest OAuth2 user request containing access token and user info
     * @return OAuth2User object with user details
     * @throws OAuth2AuthenticationException if authentication fails
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Load user from Google using default implementation
        OAuth2User oauth2User = super.loadUser(userRequest);

        try {
            // Process and save/update user in our database
            return processOAuth2User(userRequest, oauth2User);
        } catch (Exception ex) {
            logger.error("Error processing OAuth2 user: {}", ex.getMessage(), ex);
            throw new OAuth2AuthenticationException(
                new OAuth2Error("processing_error", "Error processing OAuth2 user", null),
                ex
            );
        }
    }

    /**
     * Process OAuth2 user data and create/update user in database.
     *
     * @param userRequest OAuth2 user request
     * @param oauth2User OAuth2 user from Google
     * @return OAuth2User with updated attributes
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oauth2User) {
        // Extract user attributes from Google
        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = (String) attributes.get("email");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");

        // Validate required fields
        if (email == null || email.isBlank()) {
            logger.error("Email is null or empty from Google OAuth2 response");
            throw new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info", "Email not found in OAuth2 response", null)
            );
        }

        // Use email as name if given_name is not provided
        String firstName = (givenName != null && !givenName.isBlank()) ? givenName : email.split("@")[0];
        String lastName = (familyName != null && !familyName.isBlank()) ? familyName : "";

        logger.info("Processing OAuth2 user - Email: {}, Name: {} {}", email, firstName, lastName);

        // Check if user already exists
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);

        User user;
        if (existingUserOpt.isPresent()) {
            // User exists - update to GOOGLE provider if needed
            user = existingUserOpt.get();
            logger.info("Existing user found - ID: {}, Email: {}, Current provider: {}",
                user.getId(), user.getEmail(), user.getOAuthProvider());

            // Update provider to GOOGLE if it was LOCAL
            if (user.getOAuthProvider() == OAuthProvider.LOCAL) {
                logger.info("Converting LOCAL user to GOOGLE OAuth - ID: {}", user.getId());
                user.setOAuthProvider(OAuthProvider.GOOGLE);
                user.setPassword(null); // Remove password for OAuth users
            }

            // Update user information if changed
            boolean needsUpdate = false;
            if (!firstName.equals(user.getFirstName())) {
                user.setFirstName(firstName);
                needsUpdate = true;
            }
            if (!lastName.equals(user.getLastName())) {
                user.setLastName(lastName);
                needsUpdate = true;
            }

            // Ensure user is ACTIVE and verified
            if (user.getStatus() != UserStatus.ACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
                needsUpdate = true;
            }
            if (!user.getIsVerified()) {
                user.setIsVerified(true);
                needsUpdate = true;
            }

            // Update last login timestamp
            user.setLastLoginAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());

            if (needsUpdate) {
                user = userRepository.update(user);
                logger.info("User information updated successfully - ID: {}", user.getId());
            } else {
                userRepository.updateLastLoginAt(user.getId());
                logger.info("User last login timestamp updated - ID: {}", user.getId());
            }

        } else {
            // New user - create with GOOGLE provider
            logger.info("Creating new user from Google OAuth - Email: {}", email);

            user = new User();
            user.setUuid(UUID.randomUUID());
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmail(email);
            user.setPassword(null); // OAuth users don't have password
            user.setOAuthProvider(OAuthProvider.GOOGLE);
            user.setStatus(UserStatus.ACTIVE); // OAuth users are pre-verified
            user.setRole(UserRole.USER); // Default role
            user.setIsVerified(true); // Google email is already verified
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());

            user = userRepository.save(user);
            logger.info("New Google OAuth user created successfully - ID: {}, Email: {}",
                user.getId(), user.getEmail());
        }

        // Create custom OAuth2User implementation with user ID
        return new CustomOAuth2User(oauth2User, user.getId(), user.getEmail());
    }
}
