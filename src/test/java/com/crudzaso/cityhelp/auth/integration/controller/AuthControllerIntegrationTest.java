package com.crudzaso.cityhelp.auth.integration.controller;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.infrastructure.dto.*;
import com.crudzaso.cityhelp.auth.infrastructure.security.JwtTokenProvider;
import com.crudzaso.cityhelp.auth.integration.BaseIntegrationTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for AuthController using MockMvc and Testcontainers.
 *
 * Tests all REST endpoints including:
 * - User registration and login
 * - Email verification process
 * - Token management (refresh, logout)
 * - User account operations
 * - Security and authentication
 * - Input validation and error handling
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();

        // Create test user
        testUser = createTestUser("test@example.com", "Test", "User");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIsVerified(true);
        testUser = userRepository.save(testUser);

        // Generate tokens for authenticated requests
        validAccessToken = jwtTokenProvider.generateToken(
            testUser.getId(),
            testUser.getEmail(),
            testUser.getRole().name()
        );

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(testUser.getId());
        refreshToken.setToken("refresh-token-123");
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshToken.setCreatedAt(LocalDateTime.now());
        validRefreshToken = refreshTokenRepository.save(refreshToken).getToken();
    }

    @AfterEach
    void tearDown() {
        // Clean up database by individual deletion instead of deleteAll
        cleanupTestData();
    }

    // ========== Registration Tests ==========

    @Test
    @Order(1)
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser_WithValidData() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john.doe@example.com");
        request.setPassword("Password123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario registrado. Por favor verifica tu email con el código enviado."))
                .andExpect(jsonPath("$.data.accessToken").doesNotExist())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.userInfo.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.userInfo.firstName").value("John"))
                .andExpect(jsonPath("$.data.userInfo.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.userInfo.role").value("USER"))
                .andExpect(jsonPath("$.data.userInfo.isVerified").value(false))
                .andExpect(jsonPath("$.data.userInfo.oauthProvider").value("LOCAL"));

        // Verify user was created in database
        Optional<User> createdUser = userRepository.findByEmailIgnoreCase("john.doe@example.com");
        assertThat(createdUser).isPresent();
        assertThat(createdUser.get().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(createdUser.get().getIsVerified()).isFalse();

        // Verify verification code was created
        List<EmailVerificationCode> codes = emailVerificationRepository.findByUserId(createdUser.get().getId());
        assertThat(codes).hasSize(1);
        assertThat(codes.get(0).getCode()).hasSize(6); // 6-digit code
    }

    @Test
    @DisplayName("Should return error when registering with existing email")
    void shouldReturnError_WhenRegisteringWithExistingEmail() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setEmail("test@example.com"); // Already exists
        request.setPassword("Password123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El email ya está registrado en el sistema"));
    }

    // ========== Login Tests ==========

    @Test
    @DisplayName("Should login user successfully")
    void shouldLoginUser_WithValidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("test@example.com");
        request.setPassword("Password123!");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inicio de sesión exitoso"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.userInfo.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.userInfo.isVerified").value(true));
    }

    @Test
    @DisplayName("Should return error for invalid credentials")
    void shouldReturnError_ForInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("test@example.com");
        request.setPassword("wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos"));
    }

    // ========== Current User Tests ==========

    @Test
    @DisplayName("Should get current user information with valid token")
    void shouldGetCurrentUser_WithValidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario obtenido correctamente"))
                .andExpect(jsonPath("$.data.userInfo.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.userInfo.firstName").value("Test"))
                .andExpect(jsonPath("$.data.userInfo.lastName").value("User"))
                .andExpect(jsonPath("$.data.userInfo.role").value("USER"))
                .andExpect(jsonPath("$.data.userInfo.isVerified").value(true))
                .andExpect(jsonPath("$.data.userInfo.oauthProvider").value("LOCAL"));
    }

    @Test
    @DisplayName("Should return error for missing authorization header")
    void shouldReturnError_ForMissingAuthorizationHeader() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ========== Token Refresh Tests ==========

    @Test
    @DisplayName("Should refresh access token successfully")
    void shouldRefreshAccessToken_WithValidRefreshToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(validRefreshToken);

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token renovado correctamente"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("Should return error for invalid refresh token")
    void shouldReturnError_ForInvalidRefreshToken() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        // Act & Assert
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token inválido o expirado"));
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("Should logout user successfully")
    void shouldLogoutUser_WithValidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Sesión cerrada correctamente"));

        // Verify refresh token is revoked
        List<RefreshToken> userTokens = refreshTokenRepository.findActiveByUserId(testUser.getId());
        assertThat(userTokens).isEmpty();
    }

    // ========== Delete Account Tests ==========

    @Test
    @DisplayName("Should delete user account successfully")
    void shouldDeleteUserAccount_WithValidToken() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/auth/delete-account")
                .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cuenta eliminada permanentemente"));

        // Verify user is deleted
        Optional<User> deletedUser = userRepository.findById(testUser.getId());
        assertThat(deletedUser).isEmpty();

        // Verify related entities are deleted
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(testUser.getId());
        assertThat(tokens).isEmpty();

        List<EmailVerificationCode> codes = emailVerificationRepository.findByUserId(testUser.getId());
        assertThat(codes).isEmpty();
    }

    // ========== Helper Methods ==========

    /**
     * Create a test user for integration testing.
     */
    private User createTestUser(String email, String firstName, String lastName) {
        User user = new User(firstName, lastName, email, "Password123!");
        user.setUuid(UUID.randomUUID());
        user.setRole(UserRole.USER);
        user.setOAuthProvider(OAuthProvider.LOCAL);
        return user;
    }

    /**
     * Clean up test data after each test.
     */
    private void cleanupTestData() {
        try {
            // Clean up refresh tokens
            List<RefreshToken> tokens = refreshTokenRepository.findByUserId(testUser.getId());
            for (RefreshToken token : tokens) {
                refreshTokenRepository.deleteById(token.getId());
            }

            // Clean up email verification codes
            List<EmailVerificationCode> codes = emailVerificationRepository.findByUserId(testUser.getId());
            for (EmailVerificationCode code : codes) {
                emailVerificationRepository.deleteById(code.getId());
            }

            // Clean up user
            Optional<User> user = userRepository.findById(testUser.getId());
            if (user.isPresent()) {
                userRepository.deleteById(user.get().getId());
            }

            // Clean up any additional test users created
            List<User> allUsers = userRepository.findByRole(UserRole.USER);
            for (User userToDelete : allUsers) {
                if (userToDelete.getEmail().contains("test") ||
                    userToDelete.getEmail().contains("example")) {
                    // Clean up related entities first
                    List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(userToDelete.getId());
                    for (RefreshToken token : userTokens) {
                        refreshTokenRepository.deleteById(token.getId());
                    }
                    List<EmailVerificationCode> userCodes = emailVerificationRepository.findByUserId(userToDelete.getId());
                    for (EmailVerificationCode code : userCodes) {
                        emailVerificationRepository.deleteById(code.getId());
                    }
                    userRepository.deleteById(userToDelete.getId());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the test
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }
}