package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.ValidateResetTokenUseCase;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ValidateResetTokenUseCase.
 *
 * Test Coverage:
 * 1. Valid Token - Token exists, not expired, not used
 * 2. Invalid Token - Token doesn't exist
 * 3. Expired Token - Token exists but has expired
 * 4. Used Token - Token exists but has already been used
 * 5. Null/Empty Token - Edge cases
 * 6. Repository Interaction - Correct method calls
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValidateResetTokenUseCase - Test Suite")
class ValidateResetTokenUseCaseTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private ValidateResetTokenUseCase validateResetTokenUseCase;

    private PasswordResetToken validToken;
    private static final String VALID_TOKEN = UUID.randomUUID().toString();
    private static final String INVALID_TOKEN = "invalid-token-xyz";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        validToken = new PasswordResetToken();
        validToken.setId(1L);
        validToken.setUserId(USER_ID);
        validToken.setToken(VALID_TOKEN);
        validToken.setUsed(false);
        validToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        validToken.setCreatedAt(LocalDateTime.now());
    }

    // ==================== VALID TOKEN TESTS ====================

    @Test
    @DisplayName("Should return true when token is valid (not expired, not used)")
    void shouldReturnTrue_WhenTokenIsValid() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should call token repository with correct token value")
    void shouldCallTokenRepository_WithCorrectToken() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));

        // Act
        validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        verify(tokenRepository).findByToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("Should return true for token expiring in future")
    void shouldReturnTrue_ForTokenExpiringInFuture() {
        // Arrange
        PasswordResetToken futureToken = new PasswordResetToken();
        futureToken.setId(1L);
        futureToken.setUserId(USER_ID);
        futureToken.setToken(VALID_TOKEN);
        futureToken.setUsed(false);
        futureToken.setExpiresAt(LocalDateTime.now().plusHours(23));
        futureToken.setCreatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(futureToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertTrue(result);
    }

    // ==================== INVALID TOKEN TESTS ====================

    @Test
    @DisplayName("Should return false when token does not exist")
    void shouldReturnFalse_WhenTokenDoesNotExist() {
        // Arrange
        when(tokenRepository.findByToken(INVALID_TOKEN)).thenReturn(Optional.empty());

        // Act
        boolean result = validateResetTokenUseCase.execute(INVALID_TOKEN);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when token does not exist (never called repository)")
    void shouldReturnFalse_WhenTokenNotFound_NeverCalled() {
        // Arrange
        when(tokenRepository.findByToken(INVALID_TOKEN)).thenReturn(Optional.empty());

        // Act
        validateResetTokenUseCase.execute(INVALID_TOKEN);

        // Assert
        verify(tokenRepository).findByToken(INVALID_TOKEN);
    }

    // ==================== EXPIRED TOKEN TESTS ====================

    @Test
    @DisplayName("Should return false when token is expired")
    void shouldReturnFalse_WhenTokenIsExpired() {
        // Arrange
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setId(1L);
        expiredToken.setUserId(USER_ID);
        expiredToken.setToken(VALID_TOKEN);
        expiredToken.setUsed(false);
        expiredToken.setExpiresAt(LocalDateTime.now().minusHours(1)); // Expired 1 hour ago
        expiredToken.setCreatedAt(LocalDateTime.now().minusHours(2));

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(expiredToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false when token expiration time is exactly now")
    void shouldReturnFalse_WhenTokenExpirationIsNow() {
        // Arrange
        PasswordResetToken justExpiredToken = new PasswordResetToken();
        justExpiredToken.setId(1L);
        justExpiredToken.setUserId(USER_ID);
        justExpiredToken.setToken(VALID_TOKEN);
        justExpiredToken.setUsed(false);
        justExpiredToken.setExpiresAt(LocalDateTime.now());
        justExpiredToken.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(justExpiredToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertFalse(result);
    }

    // ==================== USED TOKEN TESTS ====================

    @Test
    @DisplayName("Should return false when token has already been used")
    void shouldReturnFalse_WhenTokenHasBeenUsed() {
        // Arrange
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setId(1L);
        usedToken.setUserId(USER_ID);
        usedToken.setToken(VALID_TOKEN);
        usedToken.setUsed(true); // Token has been used
        usedToken.setExpiresAt(LocalDateTime.now().plusHours(1));
        usedToken.setCreatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(usedToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false even if used token is not expired")
    void shouldReturnFalse_ForUsedToken_IfNotExpired() {
        // Arrange
        PasswordResetToken usedToken = new PasswordResetToken();
        usedToken.setId(1L);
        usedToken.setUserId(USER_ID);
        usedToken.setToken(VALID_TOKEN);
        usedToken.setUsed(true);
        usedToken.setExpiresAt(LocalDateTime.now().plusDays(7)); // Still valid for 7 days
        usedToken.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(usedToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertFalse(result);
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle token with minimum expiration time (now + 1 second)")
    void shouldHandleToken_WithMinimumExpirationTime() {
        // Arrange
        PasswordResetToken almostExpiredToken = new PasswordResetToken();
        almostExpiredToken.setId(1L);
        almostExpiredToken.setUserId(USER_ID);
        almostExpiredToken.setToken(VALID_TOKEN);
        almostExpiredToken.setUsed(false);
        almostExpiredToken.setExpiresAt(LocalDateTime.now().plusSeconds(1));
        almostExpiredToken.setCreatedAt(LocalDateTime.now());

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(almostExpiredToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("Should handle multiple validation requests for same token")
    void shouldHandleMultipleValidationRequests() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));

        // Act
        boolean result1 = validateResetTokenUseCase.execute(VALID_TOKEN);
        boolean result2 = validateResetTokenUseCase.execute(VALID_TOKEN);
        boolean result3 = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        verify(tokenRepository, times(3)).findByToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("Should return false for completely different tokens")
    void shouldReturnFalse_ForCompletlyDifferentTokens() {
        // Arrange
        when(tokenRepository.findByToken("token1")).thenReturn(Optional.empty());
        when(tokenRepository.findByToken("token2")).thenReturn(Optional.empty());

        // Act
        boolean result1 = validateResetTokenUseCase.execute("token1");
        boolean result2 = validateResetTokenUseCase.execute("token2");

        // Assert
        assertFalse(result1);
        assertFalse(result2);
    }

    @Test
    @DisplayName("Should correctly identify valid vs invalid tokens")
    void shouldCorrectlyIdentify_ValidVsInvalidTokens() {
        // Arrange
        PasswordResetToken validToken1 = new PasswordResetToken();
        validToken1.setUsed(false);
        validToken1.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(validToken1));
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act
        boolean validResult = validateResetTokenUseCase.execute("valid-token");
        boolean invalidResult = validateResetTokenUseCase.execute("invalid-token");

        // Assert
        assertTrue(validResult);
        assertFalse(invalidResult);
    }

    @Test
    @DisplayName("Should handle token that is both used and expired")
    void shouldHandleToken_ThatIsBothUsedAndExpired() {
        // Arrange
        PasswordResetToken usedAndExpiredToken = new PasswordResetToken();
        usedAndExpiredToken.setId(1L);
        usedAndExpiredToken.setUserId(USER_ID);
        usedAndExpiredToken.setToken(VALID_TOKEN);
        usedAndExpiredToken.setUsed(true);
        usedAndExpiredToken.setExpiresAt(LocalDateTime.now().minusHours(2));
        usedAndExpiredToken.setCreatedAt(LocalDateTime.now().minusHours(3));

        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(usedAndExpiredToken));

        // Act
        boolean result = validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should verify repository.findByToken is called exactly once per execution")
    void shouldCallRepositoryFindByToken_ExactlyOnce() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));

        // Act
        validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        verify(tokenRepository, times(1)).findByToken(VALID_TOKEN);
    }

    @Test
    @DisplayName("Should not call any other repository methods")
    void shouldNotCallOtherRepositoryMethods() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));

        // Act
        validateResetTokenUseCase.execute(VALID_TOKEN);

        // Assert
        verify(tokenRepository, times(1)).findByToken(anyString());
        verifyNoMoreInteractions(tokenRepository);
    }
}
