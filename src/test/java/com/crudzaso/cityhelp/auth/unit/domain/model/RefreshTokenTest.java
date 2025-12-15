package com.crudzaso.cityhelp.auth.unit.domain.model;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RefreshToken domain entity.
 * Tests token validity, expiration, revocation, and remaining time calculation.
 *
 * Covers:
 * - Token creation and state
 * - Token validity (not revoked AND not expired)
 * - Expiration checking
 * - Expiration warnings (willExpireInHours)
 * - Remaining time calculation (getRemainingHours)
 * - Token revocation
 * - Edge cases around expiration boundaries
 */
@DisplayName("RefreshToken Entity Tests")
class RefreshTokenTest {

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    @DisplayName("Should create refresh token with correct initial state when constructed with token, userId, and expiresAt")
    void shouldCreateRefreshToken_WithValidData() {
        // Arrange
        String token = "refresh_token_abc123";
        Long userId = 1L;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // Act
        RefreshToken refreshToken = new RefreshToken(token, userId, expiresAt);

        // Assert
        assertEquals(token, refreshToken.getToken());
        assertEquals(userId, refreshToken.getUserId());
        assertEquals(expiresAt, refreshToken.getExpiresAt());
        assertNotNull(refreshToken.getCreatedAt());
        assertFalse(refreshToken.isRevoked());
    }

    @Test
    @DisplayName("Should create empty refresh token with default constructor")
    void shouldCreateEmptyRefreshToken_WithDefaultConstructor() {
        // Act
        RefreshToken refreshToken = new RefreshToken();

        // Assert
        assertNull(refreshToken.getId());
        assertNull(refreshToken.getToken());
        assertNull(refreshToken.getUserId());
        assertNull(refreshToken.getExpiresAt());
    }

    // ==================== TOKEN VALIDITY TESTS ====================

    @Test
    @DisplayName("Should be valid when token is not revoked and not expired")
    void shouldBeValid_WhenNotRevokedAndNotExpired() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);

        // Act
        boolean isValid = token.isValid();

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should be invalid when token is revoked")
    void shouldBeInvalid_WhenTokenIsRevoked() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);
        token.revoke();

        // Act
        boolean isValid = token.isValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should be invalid when token is expired")
    void shouldBeInvalid_WhenTokenIsExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        RefreshToken token = new RefreshToken("token123", 1L, pastTime);

        // Act
        boolean isValid = token.isValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should be invalid when token is both revoked and expired")
    void shouldBeInvalid_WhenTokenIsBothRevokedAndExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        RefreshToken token = new RefreshToken("token123", 1L, pastTime);
        token.revoke();

        // Act
        boolean isValid = token.isValid();

        // Assert
        assertFalse(isValid);
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    @DisplayName("Should return false when token is not expired (future expirationTime)")
    void shouldReturnFalse_WhenTokenNotExpired() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);

        // Act
        boolean isExpired = token.isExpired();

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should return true when token is expired (past expirationTime)")
    void shouldReturnTrue_WhenTokenIsExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        RefreshToken token = new RefreshToken("token123", 1L, pastTime);

        // Act
        boolean isExpired = token.isExpired();

        // Assert
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true when token is expired exactly now")
    void shouldReturnTrue_WhenTokenExpiresExactlyNow() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken("token123", 1L, now);

        // Act
        boolean isExpired = token.isExpired();

        // Assert
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return false when token expires in 1 second")
    void shouldReturnFalse_WhenTokenExpiresInFewSeconds() {
        // Arrange
        LocalDateTime almostExpired = LocalDateTime.now().plusSeconds(1);
        RefreshToken token = new RefreshToken("token123", 1L, almostExpired);

        // Act
        boolean isExpired = token.isExpired();

        // Assert
        assertFalse(isExpired);
    }

    // ==================== EXPIRATION WARNING TESTS ====================

    @Test
    @DisplayName("Should warn when token will expire in 24 hours or less")
    void shouldWarnAboutExpiration_WhenTokenExpiresWithin24Hours() {
        // Arrange
        LocalDateTime expiresIn20Hours = LocalDateTime.now().plusHours(20);
        RefreshToken token = new RefreshToken("token123", 1L, expiresIn20Hours);

        // Act
        boolean willExpire = token.willExpireInHours(24);

        // Assert
        assertTrue(willExpire);
    }

    @Test
    @DisplayName("Should not warn when token will expire after specified hours")
    void shouldNotWarnAboutExpiration_WhenTokenExpiresLater() {
        // Arrange
        LocalDateTime expiresIn3Days = LocalDateTime.now().plusDays(3);
        RefreshToken token = new RefreshToken("token123", 1L, expiresIn3Days);

        // Act
        boolean willExpire = token.willExpireInHours(24);

        // Assert
        assertFalse(willExpire);
    }

    @Test
    @DisplayName("Should warn when token expires in exactly the specified hours")
    void shouldWarnAboutExpiration_WhenTokenExpiresExactly() {
        // Arrange
        LocalDateTime expiresInExactly24Hours = LocalDateTime.now().plusHours(24);
        RefreshToken token = new RefreshToken("token123", 1L, expiresInExactly24Hours);

        // Act
        boolean willExpire = token.willExpireInHours(24);

        // Assert
        assertTrue(willExpire);
    }

    @Test
    @DisplayName("Should not warn when checking with 0 hours on a valid token (now is before expiresAt)")
    void shouldNotWarn_WhenCheckingWith0HoursOnValidToken() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusHours(100);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);

        // Act
        boolean willExpire = token.willExpireInHours(0);

        // Assert
        assertFalse(willExpire);
    }

    @Test
    @DisplayName("Should warn when checking with 0 hours on already expired token")
    void shouldWarn_WhenCheckingWith0HoursOnExpiredToken() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusHours(1);
        RefreshToken token = new RefreshToken("token123", 1L, pastTime);

        // Act
        boolean willExpire = token.willExpireInHours(0);

        // Assert
        assertTrue(willExpire);
    }

    @Test
    @DisplayName("Should warn when token is already expired")
    void shouldWarnAboutExpiration_WhenAlreadyExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        RefreshToken token = new RefreshToken("token123", 1L, pastTime);

        // Act
        boolean willExpire = token.willExpireInHours(24);

        // Assert
        assertTrue(willExpire);
    }

    // ==================== REMAINING HOURS TESTS ====================

    @Test
    @DisplayName("Should calculate remaining hours correctly when token has 7 days left")
    void shouldCalculateRemainingHours_With7DaysLeft() {
        // Arrange
        LocalDateTime expiresIn7Days = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, expiresIn7Days);

        // Act
        long remainingHours = token.getRemainingHours();

        // Assert
        // Allow 1 hour tolerance due to test execution time
        assertTrue(remainingHours >= 166 && remainingHours <= 168);
    }

    @Test
    @DisplayName("Should calculate remaining hours correctly when token has 1 hour left")
    void shouldCalculateRemainingHours_With1HourLeft() {
        // Arrange
        LocalDateTime expiresIn1Hour = LocalDateTime.now().plusHours(1);
        RefreshToken token = new RefreshToken("token123", 1L, expiresIn1Hour);

        // Act
        long remainingHours = token.getRemainingHours();

        // Assert
        assertEquals(0, remainingHours); // Less than 1 hour rounds down to 0
    }

    @Test
    @DisplayName("Should return 0 remaining hours when token is already expired")
    void shouldReturnZero_WhenTokenAlreadyExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusDays(1);
        RefreshToken token = new RefreshToken("token123", 1L, pastTime);

        // Act
        long remainingHours = token.getRemainingHours();

        // Assert
        assertEquals(0, remainingHours);
    }

    @Test
    @DisplayName("Should return 0 remaining hours when token expires exactly now")
    void shouldReturnZero_WhenTokenExpiresExactlyNow() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        RefreshToken token = new RefreshToken("token123", 1L, now);

        // Act
        long remainingHours = token.getRemainingHours();

        // Assert
        assertEquals(0, remainingHours);
    }

    @Test
    @DisplayName("Should calculate remaining hours as 24 when token expires in 1 day")
    void shouldCalculateRemainingHours_With1DayLeft() {
        // Arrange
        LocalDateTime expiresIn1Day = LocalDateTime.now().plusDays(1);
        RefreshToken token = new RefreshToken("token123", 1L, expiresIn1Day);

        // Act
        long remainingHours = token.getRemainingHours();

        // Assert
        // Allow tolerance for test execution time
        assertTrue(remainingHours >= 23 && remainingHours <= 24);
    }

    // ==================== REVOCATION TESTS ====================

    @Test
    @DisplayName("Should mark token as revoked when revoke method is called")
    void shouldRevokeToken_WhenRevokeMethodCalled() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);
        assertFalse(token.isRevoked());

        // Act
        token.revoke();

        // Assert
        assertTrue(token.isRevoked());
    }

    @Test
    @DisplayName("Should make token invalid after revocation")
    void shouldMakeTokenInvalid_AfterRevocation() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);
        assertTrue(token.isValid());

        // Act
        token.revoke();

        // Assert
        assertFalse(token.isValid());
    }

    // ==================== GETTER/SETTER TESTS ====================

    @Test
    @DisplayName("Should set and get all token fields correctly")
    void shouldSetAndGetAllFields() {
        // Arrange
        RefreshToken token = new RefreshToken();
        Long id = 1L;
        String tokenValue = "new_token_123";
        Long userId = 2L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusDays(7);

        // Act
        token.setId(id);
        token.setToken(tokenValue);
        token.setUserId(userId);
        token.setExpiresAt(futureTime);
        token.setCreatedAt(now);
        token.setRevoked(true);

        // Assert
        assertEquals(id, token.getId());
        assertEquals(tokenValue, token.getToken());
        assertEquals(userId, token.getUserId());
        assertEquals(futureTime, token.getExpiresAt());
        assertEquals(now, token.getCreatedAt());
        assertTrue(token.isRevoked());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle very long token strings")
    void shouldHandleLongTokenStrings() {
        // Arrange
        String longToken = "a".repeat(1000);
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);

        // Act
        RefreshToken token = new RefreshToken(longToken, 1L, futureTime);

        // Assert
        assertEquals(longToken, token.getToken());
    }

    @Test
    @DisplayName("Should handle very large userId values")
    void shouldHandleLargeUserIdValues() {
        // Arrange
        Long largeUserId = Long.MAX_VALUE;
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);

        // Act
        RefreshToken token = new RefreshToken("token123", largeUserId, futureTime);

        // Assert
        assertEquals(largeUserId, token.getUserId());
    }

    @Test
    @DisplayName("Should have toString method that includes key information")
    void shouldHaveToStringRepresentation() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusDays(7);
        RefreshToken token = new RefreshToken("token123", 1L, futureTime);

        // Act
        String toString = token.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("RefreshToken"));
        assertTrue(toString.contains("1"));
    }

    @Test
    @DisplayName("Should handle null expiresAt timestamp")
    void shouldHandleNullExpiresAt() {
        // Arrange
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(null);

        // Act & Assert
        assertNull(token.getExpiresAt());
    }

    @Test
    @DisplayName("Should handle null createdAt timestamp")
    void shouldHandleNullCreatedAt() {
        // Arrange
        RefreshToken token = new RefreshToken();
        token.setCreatedAt(null);

        // Act & Assert
        assertNull(token.getCreatedAt());
    }

    @Test
    @DisplayName("Should correctly identify very old tokens as expired")
    void shouldIdentifyVeryOldTokensAsExpired() {
        // Arrange
        LocalDateTime veryOldTime = LocalDateTime.now().minusYears(1);
        RefreshToken token = new RefreshToken("old_token", 1L, veryOldTime);

        // Act
        boolean isExpired = token.isExpired();
        boolean isValid = token.isValid();

        // Assert
        assertTrue(isExpired);
        assertFalse(isValid);
    }
}
