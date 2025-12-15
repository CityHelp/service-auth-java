package com.crudzaso.cityhelp.auth.infrastructure.security;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * OAuth2 Authentication Success Handler for Google login.
 * Handles successful OAuth2 authentication by generating JWT tokens and redirecting.
 *
 * Flow:
 * 1. Extract user information from OAuth2 authentication
 * 2. Generate JWT access token
 * 3. Generate JWT refresh token
 * 4. Save refresh token to database
 * 5. Redirect to frontend/mobile with tokens in URL
 *
 * @author CityHelp Team
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.oauth2.redirect-uri:${app.base-url:http://localhost:8001}/oauth2/redirect}")
    private String oauth2RedirectUri;

    @Value("${app.jwt.refresh-expiration-in-ms:604800000}") // 7 days
    private long refreshExpirationInMs;

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    /**
     * Handle successful OAuth2 authentication.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param authentication Authentication object with OAuth2 user details
     * @throws IOException if I/O error occurs
     * @throws ServletException if servlet error occurs
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (response.isCommitted()) {
            logger.debug("Response already committed. Unable to redirect.");
            return;
        }

        try {
            // Extract custom OAuth2 user
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

            // Check if it's our CustomOAuth2User
            if (!(oauth2User instanceof CustomOAuth2User)) {
                logger.error("OAuth2User is not an instance of CustomOAuth2User");
                throw new IllegalStateException("Invalid OAuth2User type");
            }

            CustomOAuth2User customOAuth2User = (CustomOAuth2User) oauth2User;
            Long userId = customOAuth2User.getUserId();
            String email = customOAuth2User.getEmail();

            logger.info("Processing OAuth2 authentication success - User ID: {}, Email: {}", userId, email);

            // Load full user from database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalStateException("User not found after OAuth2 authentication"));

            // Generate JWT access token
            String accessToken = jwtTokenProvider.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            // Generate JWT refresh token
            String refreshTokenString = generateRefreshTokenString();

            // Calculate expiration time
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpirationInMs / 1000);

            // Save refresh token to database
            RefreshToken refreshToken = new RefreshToken(refreshTokenString, user.getId(), expiresAt);
            refreshTokenRepository.save(refreshToken);

            logger.info("JWT tokens generated successfully for user ID: {}", userId);

            // Build redirect URL with tokens
            String targetUrl = buildRedirectUrl(accessToken, refreshTokenString);

            logger.info("Redirecting to: {}", oauth2RedirectUri);

            // Redirect to target URL
            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception ex) {
            logger.error("Error handling OAuth2 authentication success: {}", ex.getMessage(), ex);
            // Redirect to error page
            String errorUrl = UriComponentsBuilder.fromUriString(oauth2RedirectUri)
                    .queryParam("error", "authentication_failed")
                    .build()
                    .toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * Build redirect URL with access token and refresh token as query parameters.
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @return Redirect URL with tokens
     */
    private String buildRedirectUrl(String accessToken, String refreshToken) {
        return UriComponentsBuilder.fromUriString(oauth2RedirectUri)
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build()
                .toUriString();
    }

    /**
     * Generate a secure refresh token string.
     * Uses UUID for simplicity and uniqueness.
     *
     * @return Refresh token string
     */
    private String generateRefreshTokenString() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
