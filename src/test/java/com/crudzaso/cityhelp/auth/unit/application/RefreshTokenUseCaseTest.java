package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.RefreshTokenUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RefreshTokenUseCase.
 *
 * Test Coverage:
 * - Happy path scenarios for token refresh and generation
 * - Error scenarios for invalid/expired/revoked tokens
 * - User validation scenarios (active/verified users only)
 * - Edge cases and boundary conditions
 * - Security tests (token revocation, user status checks)
 *
 * Business Rules Tested:
 * - Only valid, non-expired, non-revoked tokens can be used
 * - User must be active and verified to refresh tokens
 * - Old refresh tokens are revoked after successful refresh
 * - Generated tokens use UUID for cryptographic security
 * - Tokens expire after configured period (7 days from application.yml)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenUseCase Tests")
class RefreshTokenUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenUseCase refreshTokenUseCase;

    private User testUser;
    private RefreshToken testRefreshToken;
    private final Long USER_ID = 1L;
    private final String REFRESH_TOKEN_VALUE = "valid-refresh-token-123";
    private final int EXPIRATION_DAYS = 7; // From application.yml: app.jwt.refresh-expiration-in-ms = 7 days

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(USER_ID);
        testUser.setUuid(UUID.randomUUID());
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@cityhelp.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIsVerified(true);
        testUser.setOAuthProvider(OAuthProvider.LOCAL);
        testUser.setCreatedAt(LocalDateTime.now().minusDays(1));

        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken(REFRESH_TOKEN_VALUE);
        testRefreshToken.setUserId(USER_ID);
        testRefreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        testRefreshToken.setCreatedAt(LocalDateTime.now().minusDays(1));
        testRefreshToken.setRevoked(false);
    }

    @Nested
    @DisplayName("execute() - Happy Path Tests")
    class ExecuteHappyPathTests {

        @Test
        @DisplayName("Should refresh token successfully with valid token and active user")
        void shouldRefreshToken_WithValidTokenAndActiveUser() {
            // Arrange
            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.update(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // Act
            User result = refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getEmail()).isEqualTo("test@cityhelp.com");
            assertThat(result.canLogin()).isTrue();

            // Verify old token is revoked
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).update(tokenCaptor.capture());
            RefreshToken revokedToken = tokenCaptor.getValue();
            assertThat(revokedToken.isRevoked()).isTrue();

            // Verify method calls
            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verify(refreshTokenRepository).update(any(RefreshToken.class));
            verifyNoMoreInteractions(refreshTokenRepository, userRepository);
        }

        @Test
        @DisplayName("Should refresh token successfully with user having OAUTH provider")
        void shouldRefreshToken_WithUserHavingOAuthProvider() {
            // Arrange
            User oauthUser = new User();
            oauthUser.setId(USER_ID);
            oauthUser.setUuid(UUID.randomUUID());
            oauthUser.setFirstName("OAuth");
            oauthUser.setLastName("User");
            oauthUser.setEmail("oauth@gmail.com");
            oauthUser.setPassword("encodedPassword");
            oauthUser.setRole(UserRole.USER);
            oauthUser.setStatus(UserStatus.ACTIVE);
            oauthUser.setIsVerified(true);
            oauthUser.setOAuthProvider(OAuthProvider.GOOGLE);
            oauthUser.setCreatedAt(LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(oauthUser));
            when(refreshTokenRepository.update(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // Act
            User result = refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);
            assertThat(result.getOAuthProvider()).isEqualTo(OAuthProvider.GOOGLE);
            assertThat(result.canLogin()).isTrue();

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verify(refreshTokenRepository).update(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should refresh token successfully when token expires exactly now")
        void shouldRefreshToken_WithTokenExpiringExactlyNow() {
            // Arrange - Token that expires right now (edge case)
            RefreshToken expiringToken = new RefreshToken();
            expiringToken.setId(1L);
            expiringToken.setToken(REFRESH_TOKEN_VALUE);
            expiringToken.setUserId(USER_ID);
            expiringToken.setExpiresAt(LocalDateTime.now().plusSeconds(1)); // Expires in 1 second
            expiringToken.setCreatedAt(LocalDateTime.now().minusDays(1));
            expiringToken.setRevoked(false);

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(expiringToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.update(any(RefreshToken.class))).thenReturn(expiringToken);

            // Act
            User result = refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verify(refreshTokenRepository).update(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("execute() - Token Validation Error Tests")
    class ExecuteTokenValidationErrorTests {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token does not exist")
        void shouldThrowInvalidCredentialsException_WhenRefreshTokenDoesNotExist() {
            // Arrange
            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid refresh token");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verifyNoInteractions(userRepository);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token is expired")
        void shouldThrowInvalidCredentialsException_WhenRefreshTokenIsExpired() {
            // Arrange
            RefreshToken expiredToken = new RefreshToken();
            expiredToken.setId(1L);
            expiredToken.setToken(REFRESH_TOKEN_VALUE);
            expiredToken.setUserId(USER_ID);
            expiredToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday
            expiredToken.setCreatedAt(LocalDateTime.now().minusDays(2));
            expiredToken.setRevoked(false);

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(expiredToken));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Refresh token has expired");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verifyNoInteractions(userRepository);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token is revoked")
        void shouldThrowInvalidCredentialsException_WhenRefreshTokenIsRevoked() {
            // Arrange
            RefreshToken revokedToken = new RefreshToken();
            revokedToken.setId(1L);
            revokedToken.setToken(REFRESH_TOKEN_VALUE);
            revokedToken.setUserId(USER_ID);
            revokedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
            revokedToken.setCreatedAt(LocalDateTime.now().minusDays(1));
            revokedToken.setRevoked(true); // Revoked

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(revokedToken));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Refresh token has been revoked");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verifyNoInteractions(userRepository);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when token is both expired and revoked")
        void shouldThrowInvalidCredentialsException_WhenTokenIsExpiredAndRevoked() {
            // Arrange
            RefreshToken expiredRevokedToken = new RefreshToken();
            expiredRevokedToken.setId(1L);
            expiredRevokedToken.setToken(REFRESH_TOKEN_VALUE);
            expiredRevokedToken.setUserId(USER_ID);
            expiredRevokedToken.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired
            expiredRevokedToken.setCreatedAt(LocalDateTime.now().minusDays(2));
            expiredRevokedToken.setRevoked(true); // Revoked

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(expiredRevokedToken));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Refresh token has expired");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verifyNoInteractions(userRepository);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException with empty token string")
        void shouldThrowInvalidCredentialsException_WithEmptyTokenString() {
            // Arrange
            when(refreshTokenRepository.findByToken("")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(""))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid refresh token");

            verify(refreshTokenRepository).findByToken("");
            verifyNoInteractions(userRepository);
            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException with null token")
        void shouldThrowInvalidCredentialsException_WithNullToken() {
            // Arrange
            when(refreshTokenRepository.findByToken(null)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(null))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Invalid refresh token");

            verify(refreshTokenRepository).findByToken(null);
            verifyNoInteractions(userRepository);
            verifyNoMoreInteractions(refreshTokenRepository);
        }
    }

    @Nested
    @DisplayName("execute() - User Validation Error Tests")
    class ExecuteUserValidationErrorTests {

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user not found for token")
        void shouldThrowInvalidCredentialsException_WhenUserNotFoundForToken() {
            // Arrange
            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("User not found");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verifyNoMoreInteractions(refreshTokenRepository, userRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user status is PENDING_VERIFICATION")
        void shouldThrowInvalidCredentialsException_WhenUserStatusIsPendingVerification() {
            // Arrange
            User pendingUser = new User();
            pendingUser.setId(USER_ID);
            pendingUser.setUuid(UUID.randomUUID());
            pendingUser.setFirstName("Pending");
            pendingUser.setLastName("User");
            pendingUser.setEmail("pending@cityhelp.com");
            pendingUser.setPassword("encodedPassword");
            pendingUser.setRole(UserRole.USER);
            pendingUser.setStatus(UserStatus.PENDING_VERIFICATION);
            pendingUser.setIsVerified(false);
            pendingUser.setOAuthProvider(OAuthProvider.LOCAL);
            pendingUser.setCreatedAt(LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(pendingUser));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("User account is not active");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verifyNoMoreInteractions(refreshTokenRepository, userRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user status is SUSPENDED")
        void shouldThrowInvalidCredentialsException_WhenUserStatusIsSuspended() {
            // Arrange
            User suspendedUser = new User();
            suspendedUser.setId(USER_ID);
            suspendedUser.setUuid(UUID.randomUUID());
            suspendedUser.setFirstName("Suspended");
            suspendedUser.setLastName("User");
            suspendedUser.setEmail("suspended@cityhelp.com");
            suspendedUser.setPassword("encodedPassword");
            suspendedUser.setRole(UserRole.USER);
            suspendedUser.setStatus(UserStatus.SUSPENDED);
            suspendedUser.setIsVerified(true);
            suspendedUser.setOAuthProvider(OAuthProvider.LOCAL);
            suspendedUser.setCreatedAt(LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(suspendedUser));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("User account is not active");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verifyNoMoreInteractions(refreshTokenRepository, userRepository);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when user email is not verified")
        void shouldThrowInvalidCredentialsException_WhenUserEmailIsNotVerified() {
            // Arrange
            User unverifiedUser = new User();
            unverifiedUser.setId(USER_ID);
            unverifiedUser.setUuid(UUID.randomUUID());
            unverifiedUser.setFirstName("Unverified");
            unverifiedUser.setLastName("User");
            unverifiedUser.setEmail("unverified@cityhelp.com");
            unverifiedUser.setPassword("encodedPassword");
            unverifiedUser.setRole(UserRole.USER);
            unverifiedUser.setStatus(UserStatus.ACTIVE);
            unverifiedUser.setIsVerified(false); // Not verified
            unverifiedUser.setOAuthProvider(OAuthProvider.LOCAL);
            unverifiedUser.setCreatedAt(LocalDateTime.now().minusDays(1));

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(unverifiedUser));

            // Act & Assert
            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("User account is not active");

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verifyNoMoreInteractions(refreshTokenRepository, userRepository);
        }
    }

    @Nested
    @DisplayName("generateNewToken() - Happy Path Tests")
    class GenerateNewTokenHappyPathTests {

        @Test
        @DisplayName("Should generate new refresh token successfully")
        void shouldGenerateNewRefreshToken_Successfully() {
            // Arrange
            RefreshToken expectedToken = new RefreshToken();
            expectedToken.setId(2L);
            expectedToken.setToken(UUID.randomUUID().toString());
            expectedToken.setUserId(USER_ID);
            expectedToken.setExpiresAt(LocalDateTime.now().plusDays(EXPIRATION_DAYS));
            expectedToken.setCreatedAt(LocalDateTime.now());
            expectedToken.setRevoked(false);

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

            // Act
            RefreshToken result = refreshTokenUseCase.generateNewToken(USER_ID, EXPIRATION_DAYS);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(USER_ID);
            assertThat(result.getToken()).isNotNull();
            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
            assertThat(result.isRevoked()).isFalse();
            assertThat(result.getCreatedAt()).isNotNull();

            // Verify token properties
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
            assertThat(savedToken.getToken()).isNotEmpty();
            assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(EXPIRATION_DAYS - 1));
            assertThat(savedToken.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(EXPIRATION_DAYS + 1));
            assertThat(savedToken.isRevoked()).isFalse();
            assertThat(savedToken.getCreatedAt()).isNotNull();

            // Verify generated token is a valid UUID
            assertDoesNotThrow(() -> UUID.fromString(savedToken.getToken()));

            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should generate new refresh token with custom expiration days")
        void shouldGenerateNewRefreshToken_WithCustomExpirationDays() {
            // Arrange
            int customExpirationDays = 14;
            RefreshToken expectedToken = new RefreshToken();
            expectedToken.setId(2L);
            expectedToken.setToken(UUID.randomUUID().toString());
            expectedToken.setUserId(USER_ID);
            expectedToken.setExpiresAt(LocalDateTime.now().plusDays(customExpirationDays));
            expectedToken.setCreatedAt(LocalDateTime.now());
            expectedToken.setRevoked(false);

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

            // Act
            RefreshToken result = refreshTokenUseCase.generateNewToken(USER_ID, customExpirationDays);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(USER_ID);

            // Verify token properties with custom expiration
            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
            assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(customExpirationDays - 1));
            assertThat(savedToken.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(customExpirationDays + 1));

            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should generate new refresh token with different user IDs")
        void shouldGenerateNewRefreshToken_WithDifferentUserIds() {
            // Arrange
            Long differentUserId = 999L;

            RefreshToken expectedToken = new RefreshToken();
            expectedToken.setId(2L);
            expectedToken.setToken(UUID.randomUUID().toString());
            expectedToken.setUserId(differentUserId);
            expectedToken.setExpiresAt(LocalDateTime.now().plusDays(EXPIRATION_DAYS));
            expectedToken.setCreatedAt(LocalDateTime.now());
            expectedToken.setRevoked(false);

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

            // Act
            RefreshToken result = refreshTokenUseCase.generateNewToken(differentUserId, EXPIRATION_DAYS);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(differentUserId);

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(differentUserId);

            verifyNoMoreInteractions(refreshTokenRepository);
        }
    }

    @Nested
    @DisplayName("generateNewToken() - Edge Cases Tests")
    class GenerateNewTokenEdgeCasesTests {

        @Test
        @DisplayName("Should generate new refresh token with minimum expiration (1 day)")
        void shouldGenerateNewRefreshToken_WithMinimumExpiration() {
            // Arrange
            int minExpirationDays = 1;

            RefreshToken expectedToken = new RefreshToken();
            expectedToken.setId(2L);
            expectedToken.setToken(UUID.randomUUID().toString());
            expectedToken.setUserId(USER_ID);
            expectedToken.setExpiresAt(LocalDateTime.now().plusDays(minExpirationDays));
            expectedToken.setCreatedAt(LocalDateTime.now());
            expectedToken.setRevoked(false);

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

            // Act
            RefreshToken result = refreshTokenUseCase.generateNewToken(USER_ID, minExpirationDays);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
            assertThat(savedToken.getExpiresAt()).isAfter(LocalDateTime.now().plusHours(20)); // Almost 1 day
            assertThat(savedToken.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(2)); // Less than 2 days

            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should generate new refresh token with zero expiration days (immediate expiration)")
        void shouldGenerateNewRefreshToken_WithZeroExpirationDays() {
            // Arrange
            int zeroExpirationDays = 0;

            RefreshToken expectedToken = new RefreshToken();
            expectedToken.setId(2L);
            expectedToken.setToken(UUID.randomUUID().toString());
            expectedToken.setUserId(USER_ID);
            expectedToken.setExpiresAt(LocalDateTime.now().plusDays(zeroExpirationDays));
            expectedToken.setCreatedAt(LocalDateTime.now());
            expectedToken.setRevoked(false);

            when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(expectedToken);

            // Act
            RefreshToken result = refreshTokenUseCase.generateNewToken(USER_ID, zeroExpirationDays);

            // Assert
            assertThat(result).isNotNull();

            ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
            verify(refreshTokenRepository).save(tokenCaptor.capture());
            RefreshToken savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getUserId()).isEqualTo(USER_ID);
            // Token should expire very soon (within next minute)
            assertThat(savedToken.getExpiresAt()).isBefore(LocalDateTime.now().plusMinutes(1));

            verifyNoMoreInteractions(refreshTokenRepository);
        }

        @Test
        @DisplayName("Should generate unique tokens for multiple calls")
        void shouldGenerateUniqueTokens_ForMultipleCalls() {
            // Arrange
            RefreshToken firstToken = new RefreshToken();
            firstToken.setId(2L);
            firstToken.setToken(UUID.randomUUID().toString());
            firstToken.setUserId(USER_ID);
            firstToken.setExpiresAt(LocalDateTime.now().plusDays(EXPIRATION_DAYS));
            firstToken.setCreatedAt(LocalDateTime.now());
            firstToken.setRevoked(false);

            RefreshToken secondToken = new RefreshToken();
            secondToken.setId(3L);
            secondToken.setToken(UUID.randomUUID().toString());
            secondToken.setUserId(USER_ID);
            secondToken.setExpiresAt(LocalDateTime.now().plusDays(EXPIRATION_DAYS));
            secondToken.setCreatedAt(LocalDateTime.now());
            secondToken.setRevoked(false);

            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenReturn(firstToken)
                    .thenReturn(secondToken);

            // Act
            RefreshToken result1 = refreshTokenUseCase.generateNewToken(USER_ID, EXPIRATION_DAYS);
            RefreshToken result2 = refreshTokenUseCase.generateNewToken(USER_ID, EXPIRATION_DAYS);

            // Assert
            assertThat(result1).isNotEqualTo(result2);
            assertThat(result1.getToken()).isNotEqualTo(result2.getToken());

            // Verify two separate save calls
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
            verifyNoMoreInteractions(refreshTokenRepository);
        }
    }

    @Nested
    @DisplayName("Security and Boundary Tests")
    class SecurityAndBoundaryTests {

        @Test
        @DisplayName("Should validate token expiration with millisecond precision")
        void shouldValidateTokenExpiration_WithMillisecondPrecision() {
            // Arrange - Token that expires in 1 millisecond
            RefreshToken barelyValidToken = new RefreshToken();
            barelyValidToken.setId(1L);
            barelyValidToken.setToken(REFRESH_TOKEN_VALUE);
            barelyValidToken.setUserId(USER_ID);
            barelyValidToken.setExpiresAt(LocalDateTime.now().plusNanos(1_000_000)); // 1 millisecond
            barelyValidToken.setCreatedAt(LocalDateTime.now().minusDays(1));
            barelyValidToken.setRevoked(false);

            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(barelyValidToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.update(any(RefreshToken.class))).thenReturn(barelyValidToken);

            // Act - This should succeed (token is still valid)
            User result = refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(USER_ID);

            verify(refreshTokenRepository).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository).findById(USER_ID);
            verify(refreshTokenRepository).update(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should prevent token reuse after successful refresh")
        void shouldPreventTokenReuse_AfterSuccessfulRefresh() {
            // Arrange
            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE)).thenReturn(Optional.of(testRefreshToken));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
            when(refreshTokenRepository.update(any(RefreshToken.class))).thenReturn(testRefreshToken);

            // Act - First refresh should succeed
            User firstResult = refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE);
            assertThat(firstResult).isNotNull();

            // Assert - Second refresh with same token should fail (revoked)
            when(refreshTokenRepository.findByToken(REFRESH_TOKEN_VALUE))
                    .thenReturn(Optional.of(testRefreshToken)); // Return same token (now revoked)

            assertThatThrownBy(() -> refreshTokenUseCase.execute(REFRESH_TOKEN_VALUE))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessage("Refresh token has been revoked");

            verify(refreshTokenRepository, times(2)).findByToken(REFRESH_TOKEN_VALUE);
            verify(userRepository, times(1)).findById(USER_ID);
            verify(refreshTokenRepository, times(1)).update(any(RefreshToken.class));
        }
    }
}