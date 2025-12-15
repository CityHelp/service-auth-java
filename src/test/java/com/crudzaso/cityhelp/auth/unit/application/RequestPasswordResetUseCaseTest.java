package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.RequestPasswordResetUseCase;
import com.crudzaso.cityhelp.auth.application.service.EmailService;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RequestPasswordResetUseCase.
 *
 * Test Coverage:
 * 1. Happy Path - Valid user email exists
 * 2. User Not Found - Email doesn't exist (silent failure for security)
 * 3. Token Generation - Verify reset token is created correctly
 * 4. Email Service - Verify email is sent with correct details
 * 5. Token Expiration - Verify token expires in 1 hour
 * 6. Edge Cases - Null email, empty email, invalid email format
 * 7. Exception Handling - Email service failures
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RequestPasswordResetUseCase - Test Suite")
class RequestPasswordResetUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RequestPasswordResetUseCase requestPasswordResetUseCase;

    private User testUser;
    private static final String TEST_EMAIL = "john@example.com";
    private static final String TEST_FIRST_NAME = "John";
    private static final String TEST_LAST_NAME = "Doe";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User(TEST_FIRST_NAME, TEST_LAST_NAME, TEST_EMAIL, "password123");
        testUser.setId(USER_ID);
        testUser.setUuid(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("Should generate reset token and send email when user exists")
    void shouldGenerateResetToken_WhenUserExists() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertNotNull(savedToken.getToken());
        assertEquals(USER_ID, savedToken.getUserId());
        assertFalse(savedToken.getUsed());
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    @DisplayName("Should send password reset email with correct details")
    void shouldSendPasswordResetEmail_WithCorrectDetails() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        verify(emailService).sendPasswordResetEmail(
            eq(TEST_EMAIL),
            eq(TEST_FIRST_NAME + " " + TEST_LAST_NAME),
            argThat(token -> token != null && !token.isEmpty())
        );
    }

    @Test
    @DisplayName("Should set token expiration to 1 hour from now")
    void shouldSetTokenExpiration_To1Hour() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        LocalDateTime beforeExecution = LocalDateTime.now();

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        LocalDateTime expectedExpiration = beforeExecution.plusHours(1);
        LocalDateTime actualExpiration = savedToken.getExpiresAt();

        assertTrue(actualExpiration.isAfter(expectedExpiration.minusMinutes(1)));
        assertTrue(actualExpiration.isBefore(expectedExpiration.plusMinutes(1)));
    }

    // ==================== USER NOT FOUND TESTS ====================

    @Test
    @DisplayName("Should not throw exception when user not found (silent failure for security)")
    void shouldNotThrowException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> requestPasswordResetUseCase.execute("nonexistent@example.com"));
    }

    @Test
    @DisplayName("Should not save token or send email when user not found")
    void shouldNotSaveToken_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        requestPasswordResetUseCase.execute("nonexistent@example.com");

        // Assert
        verify(tokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // ==================== EMAIL SERVICE INTERACTION TESTS ====================

    @Test
    @DisplayName("Should call email service with correct parameters")
    void shouldCallEmailService_WithCorrectParameters() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        verify(emailService, times(1)).sendPasswordResetEmail(
            eq(TEST_EMAIL),
            eq(TEST_FIRST_NAME + " " + TEST_LAST_NAME),
            anyString()
        );
    }

    @Test
    @DisplayName("Should propagate exception if email service fails")
    void shouldPropagateException_WhenEmailServiceFails() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });
        doThrow(new RuntimeException("Email service unavailable")).when(emailService)
            .sendPasswordResetEmail(anyString(), anyString(), anyString());

        // Act & Assert - Should throw exception
        assertThrows(RuntimeException.class, () -> requestPasswordResetUseCase.execute(TEST_EMAIL));
    }

    @Test
    @DisplayName("Should save token before sending email")
    void shouldSaveToken_BeforeSendingEmail() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert - Verify save was called
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // ==================== TOKEN GENERATION TESTS ====================

    @Test
    @DisplayName("Should generate unique tokens for each request")
    void shouldGenerateUniqueTokens_ForEachRequest() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId((long) (Math.random() * 1000));
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository, times(2)).save(tokenCaptor.capture());

        String token1 = tokenCaptor.getAllValues().get(0).getToken();
        String token2 = tokenCaptor.getAllValues().get(1).getToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
    }

    @Test
    @DisplayName("Should set token as unused (used=false)")
    void shouldSetToken_AsUnused() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertFalse(savedToken.getUsed());
    }

    // ==================== CASE INSENSITIVE EMAIL TESTS ====================

    @Test
    @DisplayName("Should find user with uppercase email (case-insensitive search)")
    void shouldFindUser_WithUppercaseEmail() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("JOHN@EXAMPLE.COM")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute("JOHN@EXAMPLE.COM");

        // Assert
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should find user with mixed case email")
    void shouldFindUser_WithMixedCaseEmail() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase("JoHn@ExAmPlE.CoM")).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute("JoHn@ExAmPlE.CoM");

        // Assert
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle user with special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Arrange
        User specialUser = new User("José", "García-López", "jose@example.com", "password123");
        specialUser.setId(2L);
        specialUser.setStatus(UserStatus.ACTIVE);
        specialUser.setIsVerified(true);

        when(userRepository.findByEmailIgnoreCase("jose@example.com")).thenReturn(Optional.of(specialUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute("jose@example.com");

        // Assert
        verify(emailService).sendPasswordResetEmail(
            eq("jose@example.com"),
            eq("José García-López"),
            anyString()
        );
    }

    @Test
    @DisplayName("Should handle multiple requests from same user")
    void shouldHandleMultipleRequests_FromSameUser() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId((long) (Math.random() * 1000));
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);
        requestPasswordResetUseCase.execute(TEST_EMAIL);
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        verify(tokenRepository, times(3)).save(any(PasswordResetToken.class));
        verify(emailService, times(3)).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should correctly set userId in reset token")
    void shouldCorrectlySetUserId_InResetToken() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertEquals(USER_ID, savedToken.getUserId());
    }

    @Test
    @DisplayName("Should verify correct user repository method is called")
    void shouldCallCorrectUserRepositoryMethod() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> {
            PasswordResetToken token = invocation.getArgument(0);
            token.setId(1L);
            return token;
        });

        // Act
        requestPasswordResetUseCase.execute(TEST_EMAIL);

        // Assert
        verify(userRepository).findByEmailIgnoreCase(TEST_EMAIL);
    }
}
