package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.ResetPasswordUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetPasswordUseCase - Test Suite")
class ResetPasswordUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordUseCase resetPasswordUseCase;

    private User testUser;
    private PasswordResetToken validToken;
    private static final Long USER_ID = 1L;
    private static final String VALID_TOKEN = "valid-token-123";
    private static final String NEW_PASSWORD = "newPassword123!";
    private static final String ENCODED_PASSWORD = "encoded-password-bcrypt";

    @BeforeEach
    void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "password123");
        testUser.setId(USER_ID);
        testUser.setUuid(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);

        validToken = new PasswordResetToken(
            USER_ID,
            VALID_TOKEN,
            LocalDateTime.now().plusHours(1)
        );
    }

    // ==================== HAPPY PATH TESTS ====================

    @Test
    @DisplayName("Should reset password successfully with valid token")
    void shouldResetPassword_WithValidToken() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        // Act
        resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD);

        // Assert
        verify(userRepository).update(any(User.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Should encode password before updating user")
    void shouldEncodePassword_BeforeUpdatingUser() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.update(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        // Act
        resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD);

        // Assert
        verify(passwordEncoder).encode(NEW_PASSWORD);
    }

    @Test
    @DisplayName("Should mark token as used after successful reset")
    void shouldMarkTokenAsUsed_AfterSuccessfulReset() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        // Act
        resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD);

        // Assert
        verify(tokenRepository).save(argThat(token -> token.getUsed()));
    }

    @Test
    @DisplayName("Should call token repository to find token by value")
    void shouldCallTokenRepository_ToFindTokenByValue() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        // Act
        resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD);

        // Assert
        verify(tokenRepository).findByToken(VALID_TOKEN);
    }

    // ==================== INVALID TOKEN TESTS ====================

    @Test
    @DisplayName("Should throw exception when token does not exist")
    void shouldThrowException_WhenTokenDoesNotExist() {
        // Arrange
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("invalid-token", NEW_PASSWORD)
        );
    }

    @Test
    @DisplayName("Should throw InvalidTokenException with specific message when token not found")
    void shouldThrowInvalidTokenException_WithSpecificMessage() {
        // Arrange
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        InvalidTokenException exception = assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("invalid-token", NEW_PASSWORD)
        );

        assertThat(exception.getMessage()).isNotNull();
    }

    // ==================== EXPIRED TOKEN TESTS ====================

    @Test
    @DisplayName("Should throw exception when token is expired")
    void shouldThrowException_WhenTokenIsExpired() {
        // Arrange
        PasswordResetToken expiredToken = new PasswordResetToken(
            USER_ID,
            "expired-token",
            LocalDateTime.now().minusHours(1)
        );

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("expired-token", NEW_PASSWORD)
        );
    }

    @Test
    @DisplayName("Should throw exception when token was already used")
    void shouldThrowException_WhenTokenWasAlreadyUsed() {
        // Arrange
        PasswordResetToken usedToken = new PasswordResetToken(
            USER_ID,
            "used-token",
            LocalDateTime.now().plusHours(1)
        );
        usedToken.setUsed(true);

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("used-token", NEW_PASSWORD)
        );
    }

    @Test
    @DisplayName("Should throw exception when token is expired and used")
    void shouldThrowException_WhenTokenIsExpiredAndUsed() {
        // Arrange
        PasswordResetToken expiredUsedToken = new PasswordResetToken(
            USER_ID,
            "expired-used-token",
            LocalDateTime.now().minusHours(1)
        );
        expiredUsedToken.setUsed(true);

        when(tokenRepository.findByToken("expired-used-token")).thenReturn(Optional.of(expiredUsedToken));

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("expired-used-token", NEW_PASSWORD)
        );
    }

    // ==================== USER NOT FOUND TESTS ====================

    @Test
    @DisplayName("Should throw exception when user associated with token is not found")
    void shouldThrowException_WhenUserNotFound() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD)
        );
    }

    // ==================== PASSWORD UPDATE TESTS ====================

    @Test
    @DisplayName("Should update user with new encoded password")
    void shouldUpdateUser_WithNewEncodedPassword() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.update(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        // Act
        resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD);

        // Assert
        verify(userRepository).update(argThat(user ->
            user.getId().equals(USER_ID) &&
            user.getPassword().equals(ENCODED_PASSWORD)
        ));
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle token expiring exactly at current time")
    void shouldThrowException_WhenTokenExpiresAtCurrentTime() {
        // Arrange
        PasswordResetToken justExpiredToken = new PasswordResetToken(
            USER_ID,
            "just-expired-token",
            LocalDateTime.now()
        );

        when(tokenRepository.findByToken("just-expired-token")).thenReturn(Optional.of(justExpiredToken));

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("just-expired-token", NEW_PASSWORD)
        );
    }

    @Test
    @DisplayName("Should not call user repository when token is invalid")
    void shouldNotCallUserRepository_WhenTokenIsInvalid() {
        // Arrange
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("invalid-token", NEW_PASSWORD)
        );

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should not encode password when token is invalid")
    void shouldNotEncodePassword_WhenTokenIsInvalid() {
        // Arrange
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("invalid-token", NEW_PASSWORD)
        );

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should verify repository method call order")
    void shouldFollowCorrectExecutionOrder() {
        // Arrange
        when(tokenRepository.findByToken(VALID_TOKEN)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        // Act
        resetPasswordUseCase.execute(VALID_TOKEN, NEW_PASSWORD);

        // Assert - Verify calls happened in order
        verify(tokenRepository).findByToken(VALID_TOKEN);
        verify(userRepository).findById(USER_ID);
        verify(passwordEncoder).encode(NEW_PASSWORD);
        verify(userRepository).update(any(User.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }
}
