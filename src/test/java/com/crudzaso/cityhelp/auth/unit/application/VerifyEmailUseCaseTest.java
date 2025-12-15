package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.InvalidVerificationCodeException;
import com.crudzaso.cityhelp.auth.application.VerifyEmailUseCase;
import com.crudzaso.cityhelp.auth.application.service.EmailService;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

/**
 * Comprehensive unit tests for VerifyEmailUseCase with 90%+ coverage.
 *
 * Test Categories:
 * 1. Happy Paths - Successful email verification scenarios
 * 2. Error Handling - Invalid/expired codes, user not found, etc.
 * 3. Edge Cases - Maximum attempts, null handling, error scenarios
 * 4. Input Validation - Null/empty input handling
 * 5. Repository Interactions - Proper data operations
 * 6. Business Logic - Code expiration, attempt limits, etc.
 * 7. Email Integration - Welcome email handling
 *
 * Target Coverage: 90%+ with exhaustive test scenarios (16 tests)
 */
@DisplayName("VerifyEmailUseCase Tests")
class VerifyEmailUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerifyEmailUseCase verifyEmailUseCase;

    private User testUser;
    private EmailVerificationCode testVerificationCode;
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final String TEST_VERIFICATION_CODE = "123456";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test user
        testUser = new User();
        testUser.setId(TEST_USER_ID);
        testUser.setEmail(TEST_EMAIL);
        testUser.setFirstName(TEST_FIRST_NAME);
        testUser.setLastName(TEST_LAST_NAME);
        testUser.setStatus(UserStatus.PENDING_VERIFICATION);

        // Setup test verification code
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(10);
        testVerificationCode = new EmailVerificationCode(TEST_USER_ID, TEST_VERIFICATION_CODE, futureTime);
        testVerificationCode.setId(100L);
    }

    @Test
    @DisplayName("Should verify email successfully with valid code")
    void shouldVerifyEmail_WithValidCode() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        doNothing().when(emailVerificationRepository).markAsUsedById(100L);

        // Act
        boolean result = verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE);

        // Assert
        assertTrue(result);
        verify(userRepository).findById(TEST_USER_ID);
        verify(emailVerificationRepository).findLatestByUserId(TEST_USER_ID);
        verify(emailVerificationRepository).markAsUsedById(100L);
        verify(userRepository).updateStatus(TEST_USER_ID, UserStatus.ACTIVE);
        verify(emailService).sendWelcomeEmail(TEST_EMAIL, TEST_FIRST_NAME + " " + TEST_LAST_NAME);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE)
        );

        assertTrue(exception.getMessage().contains("User not found for ID"));
        verify(userRepository).findById(TEST_USER_ID);
        verifyNoInteractions(emailVerificationRepository);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw exception when no verification code found")
    void shouldThrowException_WhenNoVerificationCodeFound() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.empty());

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE)
        );

        assertTrue(exception.getMessage().contains("No verification code found"));
        verify(userRepository).findById(TEST_USER_ID);
        verify(emailVerificationRepository).findLatestByUserId(TEST_USER_ID);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw exception when verification code has expired")
    void shouldThrowException_WhenVerificationCodeExpired() {
        // Arrange - Code expired 1 minute ago
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        testVerificationCode.setExpiresAt(pastTime);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE)
        );

        assertTrue(exception.getMessage().contains("expired or is invalid"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw exception when verification code is already used")
    void shouldThrowException_WhenVerificationCodeAlreadyUsed() {
        // Arrange
        testVerificationCode.setUsed(true);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE)
        );

        assertTrue(exception.getMessage().contains("already been used"));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw exception when verification code is invalid")
    void shouldThrowException_WhenVerificationCodeInvalid() {
        // Arrange
        String invalidCode = "999999";

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, invalidCode)
        );

        assertTrue(exception.getMessage().contains("Invalid verification code"));
        verify(emailVerificationRepository).update(testVerificationCode);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should throw exception when maximum attempts exceeded")
    void shouldThrowException_WhenMaximumAttemptsExceeded() {
        // Arrange - Code has 2 attempts already, this will be the 3rd invalid attempt
        String invalidCode = "999999";
        testVerificationCode.setAttempts(2);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, invalidCode)
        );

        assertTrue(exception.getMessage().contains("Maximum verification attempts exceeded"));
        verify(emailVerificationRepository).update(testVerificationCode);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should continue verification even when welcome email fails")
    void shouldContinueVerification_EvenWhenWelcomeEmailFails() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        doNothing().when(emailVerificationRepository).markAsUsedById(100L);
        doThrow(new RuntimeException("SMTP server down"))
            .when(emailService).sendWelcomeEmail(anyString(), anyString());

        // Act
        boolean result = verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE);

        // Assert - Verification should still succeed despite email failure
        assertTrue(result);
        verify(emailVerificationRepository).markAsUsedById(100L);
        verify(userRepository).updateStatus(TEST_USER_ID, UserStatus.ACTIVE);
        verify(emailService).sendWelcomeEmail(TEST_EMAIL, TEST_FIRST_NAME + " " + TEST_LAST_NAME);
    }

    @Test
    @DisplayName("Should update user status after successful verification")
    void shouldUpdateUserStatus_AfterSuccessfulVerification() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        doNothing().when(emailVerificationRepository).markAsUsedById(100L);

        // Act
        boolean result = verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE);

        // Assert
        assertTrue(result);
        verify(userRepository).updateStatus(TEST_USER_ID, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should mark verification code as used after successful verification")
    void shouldMarkVerificationCodeAsUsed_AfterSuccessfulVerification() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        doNothing().when(emailVerificationRepository).markAsUsedById(100L);

        // Act
        boolean result = verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE);

        // Assert
        assertTrue(result);
        verify(emailVerificationRepository).markAsUsedById(100L);
    }

    @Test
    @DisplayName("Should send welcome email with correct parameters")
    void shouldSendWelcomeEmail_WithCorrectParameters() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        doNothing().when(emailVerificationRepository).markAsUsedById(100L);

        // Act
        boolean result = verifyEmailUseCase.execute(TEST_USER_ID, TEST_VERIFICATION_CODE);

        // Assert
        assertTrue(result);
        verify(emailService).sendWelcomeEmail(eq(TEST_EMAIL), eq(TEST_FIRST_NAME + " " + TEST_LAST_NAME));
    }

    @Test
    @DisplayName("Should not update user status when verification fails")
    void shouldNotUpdateUserStatus_WhenVerificationFails() {
        // Arrange
        String invalidCode = "999999";

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act
        assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, invalidCode)
        );

        // Assert
        verify(userRepository, never()).updateStatus(anyLong(), any());
        verify(emailVerificationRepository, never()).markAsUsedById(anyLong());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should not mark code as used when verification fails")
    void shouldNotMarkCodeAsUsed_WhenVerificationFails() {
        // Arrange
        String invalidCode = "999999";

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act
        assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, invalidCode)
        );

        // Assert
        verify(emailVerificationRepository, never()).markAsUsedById(anyLong());
        verify(emailVerificationRepository, never()).markAsUsed(any());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should increment attempts on invalid code")
    void shouldIncrementAttempts_OnInvalidCode() {
        // Arrange
        String invalidCode = "999999";
        testVerificationCode.setAttempts(1);

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act
        assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, invalidCode)
        );

        // Assert
        ArgumentCaptor<EmailVerificationCode> codeCaptor = ArgumentCaptor.forClass(EmailVerificationCode.class);
        verify(emailVerificationRepository).update(codeCaptor.capture());

        EmailVerificationCode updatedCode = codeCaptor.getValue();
        assertEquals(2, updatedCode.getAttempts()); // Incremented from 1 to 2
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should handle null code gracefully")
    void shouldHandleNullCode_Gracefully() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, null)
        );

        assertTrue(exception.getMessage().contains("Invalid verification code"));
        verify(emailVerificationRepository).update(testVerificationCode);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should handle empty code gracefully")
    void shouldHandleEmptyCode_Gracefully() {
        // Arrange
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act & Assert
        InvalidVerificationCodeException exception = assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, "")
        );

        assertTrue(exception.getMessage().contains("Invalid verification code"));
        verify(emailVerificationRepository).update(testVerificationCode);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Should not send welcome email when verification fails")
    void shouldNotSendWelcomeEmail_WhenVerificationFails() {
        // Arrange
        String invalidCode = "999999";

        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(emailVerificationRepository.findLatestByUserId(TEST_USER_ID))
            .thenReturn(Optional.of(testVerificationCode));
        when(emailVerificationRepository.update(testVerificationCode))
            .thenReturn(testVerificationCode);

        // Act
        assertThrows(
            InvalidVerificationCodeException.class,
            () -> verifyEmailUseCase.execute(TEST_USER_ID, invalidCode)
        );

        // Assert
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }
}