package com.crudzaso.cityhelp.auth.unit.domain.model;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EmailVerificationCode domain entity.
 * Tests code validity, expiration, usage tracking, attempt management, and masking.
 *
 * Covers:
 * - Code creation and state
 * - Code validity (not used AND not expired AND attempts < 3)
 * - Expiration checking
 * - Expiration warnings (willExpireInMinutes)
 * - Remaining time calculation (getRemainingMinutes)
 * - Code usage marking
 * - Attempt tracking and limits
 * - Code masking for security
 * - Edge cases around expiration and attempt boundaries
 */
@DisplayName("EmailVerificationCode Entity Tests")
class EmailVerificationCodeTest {

    private static final String VALID_CODE = "123456";
    private static final String SHORT_CODE = "12";
    private static final String LONG_CODE = "123456789";

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    @DisplayName("Should create email verification code with correct initial state")
    void shouldCreateEmailVerificationCode_WithValidData() {
        // Arrange
        Long userId = 1L;
        String code = VALID_CODE;
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

        // Act
        EmailVerificationCode verificationCode = new EmailVerificationCode(userId, code, expiresAt);

        // Assert
        assertEquals(userId, verificationCode.getUserId());
        assertEquals(code, verificationCode.getCode());
        assertEquals(expiresAt, verificationCode.getExpiresAt());
        assertNotNull(verificationCode.getCreatedAt());
        assertFalse(verificationCode.isUsed());
        assertEquals(0, verificationCode.getAttempts());
    }

    @Test
    @DisplayName("Should create empty email verification code with default constructor")
    void shouldCreateEmptyEmailVerificationCode_WithDefaultConstructor() {
        // Act
        EmailVerificationCode verificationCode = new EmailVerificationCode();

        // Assert
        assertNull(verificationCode.getId());
        assertNull(verificationCode.getUserId());
        assertNull(verificationCode.getCode());
        assertNull(verificationCode.getExpiresAt());
    }

    // ==================== CODE VALIDITY TESTS ====================

    @Test
    @DisplayName("Should be valid when code is not used, not expired, and attempts < 3")
    void shouldBeValid_WhenAllConditionsMet() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act
        boolean isValid = code.isValid();

        // Assert
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should be invalid when code has been used")
    void shouldBeInvalid_WhenCodeIsUsed() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        code.markAsUsed();

        // Act
        boolean isValid = code.isValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should be invalid when code is expired")
    void shouldBeInvalid_WhenCodeIsExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, pastTime);

        // Act
        boolean isValid = code.isValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should be invalid when attempts have exceeded limit (3)")
    void shouldBeInvalid_WhenAttemptsExceeded() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        code.incrementAttempts(); // 1
        code.incrementAttempts(); // 2
        code.incrementAttempts(); // 3

        // Act
        boolean isValid = code.isValid();

        // Assert
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should be invalid when code is used, expired, and attempts exceeded")
    void shouldBeInvalid_WhenAllConditionsFail() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, pastTime);
        code.markAsUsed();
        code.incrementAttempts();
        code.incrementAttempts();
        code.incrementAttempts();

        // Act
        boolean isValid = code.isValid();

        // Assert
        assertFalse(isValid);
    }

    // ==================== EXPIRATION TESTS ====================

    @Test
    @DisplayName("Should return false when code is not expired")
    void shouldReturnFalse_WhenCodeNotExpired() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act
        boolean isExpired = code.isExpired();

        // Assert
        assertFalse(isExpired);
    }

    @Test
    @DisplayName("Should return true when code is expired")
    void shouldReturnTrue_WhenCodeIsExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, pastTime);

        // Act
        boolean isExpired = code.isExpired();

        // Assert
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return true when code expires exactly now")
    void shouldReturnTrue_WhenCodeExpiresExactlyNow() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, now);

        // Act
        boolean isExpired = code.isExpired();

        // Assert
        assertTrue(isExpired);
    }

    @Test
    @DisplayName("Should return false when code expires in 1 second")
    void shouldReturnFalse_WhenCodeExpiresInFewSeconds() {
        // Arrange
        LocalDateTime almostExpired = LocalDateTime.now().plusSeconds(1);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, almostExpired);

        // Act
        boolean isExpired = code.isExpired();

        // Assert
        assertFalse(isExpired);
    }

    // ==================== EXPIRATION WARNING TESTS ====================

    @Test
    @DisplayName("Should warn when code will expire in 5 minutes or less")
    void shouldWarnAboutExpiration_WhenCodeExpiresWithin5Minutes() {
        // Arrange
        LocalDateTime expiresIn3Minutes = LocalDateTime.now().plusMinutes(3);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, expiresIn3Minutes);

        // Act
        boolean willExpire = code.willExpireInMinutes(5);

        // Assert
        assertTrue(willExpire);
    }

    @Test
    @DisplayName("Should not warn when code will expire after specified minutes")
    void shouldNotWarnAboutExpiration_WhenCodeExpiresLater() {
        // Arrange
        LocalDateTime expiresIn20Minutes = LocalDateTime.now().plusMinutes(20);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, expiresIn20Minutes);

        // Act
        boolean willExpire = code.willExpireInMinutes(5);

        // Assert
        assertFalse(willExpire);
    }

    @Test
    @DisplayName("Should warn when code expires in exactly the specified minutes")
    void shouldWarnAboutExpiration_WhenCodeExpiresExactly() {
        // Arrange
        LocalDateTime expiresInExactly5Minutes = LocalDateTime.now().plusMinutes(5);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, expiresInExactly5Minutes);

        // Act
        boolean willExpire = code.willExpireInMinutes(5);

        // Assert
        assertTrue(willExpire);
    }

    @Test
    @DisplayName("Should not warn when checking with 0 minutes on a valid code (now is before expiresAt)")
    void shouldNotWarn_WhenCheckingWith0MinutesOnValidCode() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(100);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act
        boolean willExpire = code.willExpireInMinutes(0);

        // Assert
        assertFalse(willExpire);
    }

    @Test
    @DisplayName("Should warn when checking with 0 minutes on already expired code")
    void shouldWarn_WhenCheckingWith0MinutesOnExpiredCode() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, pastTime);

        // Act
        boolean willExpire = code.willExpireInMinutes(0);

        // Assert
        assertTrue(willExpire);
    }

    @Test
    @DisplayName("Should warn when code is already expired")
    void shouldWarnAboutExpiration_WhenAlreadyExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, pastTime);

        // Act
        boolean willExpire = code.willExpireInMinutes(5);

        // Assert
        assertTrue(willExpire);
    }

    // ==================== REMAINING MINUTES TESTS ====================

    @Test
    @DisplayName("Should calculate remaining minutes correctly when code has 15 minutes left")
    void shouldCalculateRemainingMinutes_With15MinutesLeft() {
        // Arrange
        LocalDateTime expiresIn15Minutes = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, expiresIn15Minutes);

        // Act
        long remainingMinutes = code.getRemainingMinutes();

        // Assert
        // Allow 1 minute tolerance due to test execution time
        assertTrue(remainingMinutes >= 14 && remainingMinutes <= 15);
    }

    @Test
    @DisplayName("Should calculate remaining minutes correctly when code has 1 minute left")
    void shouldCalculateRemainingMinutes_With1MinuteLeft() {
        // Arrange
        LocalDateTime expiresIn1Minute = LocalDateTime.now().plusMinutes(1);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, expiresIn1Minute);

        // Act
        long remainingMinutes = code.getRemainingMinutes();

        // Assert
        assertEquals(0, remainingMinutes); // Less than 1 minute rounds down to 0
    }

    @Test
    @DisplayName("Should return 0 remaining minutes when code is already expired")
    void shouldReturnZero_WhenCodeAlreadyExpired() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, pastTime);

        // Act
        long remainingMinutes = code.getRemainingMinutes();

        // Assert
        assertEquals(0, remainingMinutes);
    }

    @Test
    @DisplayName("Should return 0 remaining minutes when code expires exactly now")
    void shouldReturnZero_WhenCodeExpiresExactlyNow() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, now);

        // Act
        long remainingMinutes = code.getRemainingMinutes();

        // Assert
        assertEquals(0, remainingMinutes);
    }

    // ==================== USAGE TRACKING TESTS ====================

    @Test
    @DisplayName("Should mark code as used when markAsUsed method is called")
    void shouldMarkCodeAsUsed_WhenMethodCalled() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        assertFalse(code.isUsed());

        // Act
        code.markAsUsed();

        // Assert
        assertTrue(code.isUsed());
    }

    @Test
    @DisplayName("Should make code invalid after marking as used")
    void shouldMakeCodeInvalid_AfterMarkingAsUsed() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        assertTrue(code.isValid());

        // Act
        code.markAsUsed();

        // Assert
        assertFalse(code.isValid());
    }

    // ==================== ATTEMPT TRACKING TESTS ====================

    @Test
    @DisplayName("Should increment attempts counter correctly")
    void shouldIncrementAttempts_WhenMethodCalled() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        assertEquals(0, code.getAttempts());

        // Act
        code.incrementAttempts();
        code.incrementAttempts();
        code.incrementAttempts();

        // Assert
        assertEquals(3, code.getAttempts());
    }

    @Test
    @DisplayName("Should allow up to 2 attempts before exceeding limit")
    void shouldAllowUpTo2Attempts_BeforeExceedingLimit() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act - First attempt
        code.incrementAttempts();
        boolean isValid1 = code.isValid();

        // Second attempt
        code.incrementAttempts();
        boolean isValid2 = code.isValid();

        // Third attempt
        code.incrementAttempts();
        boolean isValid3 = code.isValid();

        // Assert
        assertTrue(isValid1);
        assertTrue(isValid2);
        assertFalse(isValid3);
    }

    @Test
    @DisplayName("Should return true when attempts have exceeded limit (hasExceededAttempts)")
    void shouldReturnTrue_WhenAttemptsExceededLimit() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        code.incrementAttempts();
        code.incrementAttempts();
        code.incrementAttempts();

        // Act
        boolean hasExceeded = code.hasExceededAttempts();

        // Assert
        assertTrue(hasExceeded);
    }

    @Test
    @DisplayName("Should return false when attempts have not exceeded limit (hasExceededAttempts)")
    void shouldReturnFalse_WhenAttemptsNotExceededLimit() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);
        code.incrementAttempts();
        code.incrementAttempts();

        // Act
        boolean hasExceeded = code.hasExceededAttempts();

        // Assert
        assertFalse(hasExceeded);
    }

    @Test
    @DisplayName("Should return false when attempts is 0")
    void shouldReturnFalse_WhenAttemptsIsZero() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act
        boolean hasExceeded = code.hasExceededAttempts();

        // Assert
        assertFalse(hasExceeded);
    }

    // ==================== CODE MASKING TESTS ====================

    @Test
    @DisplayName("Should mask valid 6-digit code correctly (XX****YY format)")
    void shouldMaskValidCode_With6Digits() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act
        String masked = code.maskCode();

        // Assert
        assertEquals("12****56", masked);
    }

    @Test
    @DisplayName("Should return asterisks mask when code is null")
    void shouldReturnAsterisks_WhenCodeIsNull() {
        // Arrange
        EmailVerificationCode code = new EmailVerificationCode();
        code.setCode(null);

        // Act
        String masked = code.maskCode();

        // Assert
        assertEquals("******", masked);
    }

    @Test
    @DisplayName("Should return asterisks mask when code is shorter than 6 digits")
    void shouldReturnAsterisks_WhenCodeIsTooShort() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, SHORT_CODE, futureTime);

        // Act
        String masked = code.maskCode();

        // Assert
        assertEquals("******", masked);
    }

    @Test
    @DisplayName("Should return asterisks mask when code is longer than 6 digits")
    void shouldReturnAsterisks_WhenCodeIsTooLong() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, LONG_CODE, futureTime);

        // Act
        String masked = code.maskCode();

        // Assert
        assertEquals("******", masked);
    }

    @Test
    @DisplayName("Should mask code with all same digits")
    void shouldMaskCode_WithAllSameDigits() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, "111111", futureTime);

        // Act
        String masked = code.maskCode();

        // Assert
        assertEquals("11****11", masked);
    }

    @Test
    @DisplayName("Should mask code with alphanumeric characters")
    void shouldMaskCode_WithAlphanumericCharacters() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, "ABC123", futureTime);

        // Act
        String masked = code.maskCode();

        // Assert
        assertEquals("AB****23", masked);
    }

    // ==================== GETTER/SETTER TESTS ====================

    @Test
    @DisplayName("Should set and get all code fields correctly")
    void shouldSetAndGetAllFields() {
        // Arrange
        EmailVerificationCode code = new EmailVerificationCode();
        Long id = 1L;
        Long userId = 2L;
        String codeValue = VALID_CODE;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureTime = now.plusMinutes(15);

        // Act
        code.setId(id);
        code.setUserId(userId);
        code.setCode(codeValue);
        code.setExpiresAt(futureTime);
        code.setCreatedAt(now);
        code.setUsed(true);
        code.setAttempts(2);

        // Assert
        assertEquals(id, code.getId());
        assertEquals(userId, code.getUserId());
        assertEquals(codeValue, code.getCode());
        assertEquals(futureTime, code.getExpiresAt());
        assertEquals(now, code.getCreatedAt());
        assertTrue(code.isUsed());
        assertEquals(2, code.getAttempts());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle empty string code")
    void shouldHandleEmptyStringCode() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);

        // Act
        EmailVerificationCode code = new EmailVerificationCode(1L, "", futureTime);

        // Assert
        assertEquals("", code.getCode());
        assertEquals("******", code.maskCode());
    }

    @Test
    @DisplayName("Should handle very large userId values")
    void shouldHandleLargeUserIdValues() {
        // Arrange
        Long largeUserId = Long.MAX_VALUE;
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);

        // Act
        EmailVerificationCode code = new EmailVerificationCode(largeUserId, VALID_CODE, futureTime);

        // Assert
        assertEquals(largeUserId, code.getUserId());
    }

    @Test
    @DisplayName("Should have toString method that includes masked code")
    void shouldHaveToStringRepresentation() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(15);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, futureTime);

        // Act
        String toString = code.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("EmailVerificationCode"));
        assertTrue(toString.contains("12****56")); // Should contain masked code
    }

    @Test
    @DisplayName("Should handle null expiresAt timestamp")
    void shouldHandleNullExpiresAt() {
        // Arrange
        EmailVerificationCode code = new EmailVerificationCode();
        code.setExpiresAt(null);

        // Act & Assert
        assertNull(code.getExpiresAt());
    }

    @Test
    @DisplayName("Should correctly identify very old codes as expired")
    void shouldIdentifyVeryOldCodesAsExpired() {
        // Arrange
        LocalDateTime veryOldTime = LocalDateTime.now().minusYears(1);
        EmailVerificationCode code = new EmailVerificationCode(1L, VALID_CODE, veryOldTime);

        // Act
        boolean isExpired = code.isExpired();
        boolean isValid = code.isValid();

        // Assert
        assertTrue(isExpired);
        assertFalse(isValid);
    }
}
