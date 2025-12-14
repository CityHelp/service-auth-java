package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.ResetPasswordUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ResetPasswordUseCase.
 * Tests the password reset flow with valid and invalid tokens.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResetPasswordUseCase Tests")
class ResetPasswordUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    private PasswordEncoder passwordEncoder;
    private ResetPasswordUseCase resetPasswordUseCase;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        resetPasswordUseCase = new ResetPasswordUseCase(
                userRepository,
                tokenRepository,
                passwordEncoder
        );
    }

    @Test
    @DisplayName("Should reset password successfully with valid token")
    void testResetPasswordSuccess() {
        // Arrange
        String token = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        Long userId = 1L;

        User user = createTestUser(userId, "test@example.com");
        PasswordResetToken resetToken = createValidResetToken(token, userId);

        when(tokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
        when(userRepository.update(any(User.class)))
                .thenReturn(user);
        when(tokenRepository.save(any(PasswordResetToken.class)))
                .thenReturn(resetToken);

        // Act
        assertDoesNotThrow(() -> resetPasswordUseCase.execute(token, newPassword));

        // Assert
        verify(userRepository).findById(userId);
        verify(userRepository).update(any(User.class));
        verify(tokenRepository).save(resetToken);
        assertTrue(resetToken.getUsed(), "Token should be marked as used");
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token not found")
    void testResetPasswordTokenNotFound() {
        String token = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";

        when(tokenRepository.findByToken(token))
                .thenReturn(Optional.empty());

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> resetPasswordUseCase.execute(token, newPassword)
        );

        assertEquals("Token no válido", exception.getMessage());
        verify(userRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token is expired")
    void testResetPasswordExpiredToken() {
        String token = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        Long userId = 1L;

        PasswordResetToken expiredToken = new PasswordResetToken(
                userId,
                token,
                LocalDateTime.now(ZoneOffset.UTC).minusHours(1)
        );

        when(tokenRepository.findByToken(token))
                .thenReturn(Optional.of(expiredToken));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> resetPasswordUseCase.execute(token, newPassword)
        );

        assertEquals("Token expirado o ya utilizado", exception.getMessage());
        verify(userRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token already used")
    void testResetPasswordAlreadyUsedToken() {
        String token = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        Long userId = 1L;

        PasswordResetToken usedToken = createValidResetToken(token, userId);
        usedToken.setUsed(true);

        when(tokenRepository.findByToken(token))
                .thenReturn(Optional.of(usedToken));

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> resetPasswordUseCase.execute(token, newPassword)
        );

        assertEquals("Token expirado o ya utilizado", exception.getMessage());
        verify(userRepository, never()).update(any());
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when user not found")
    void testResetPasswordUserNotFound() {
        String token = UUID.randomUUID().toString();
        String newPassword = "NewPassword123!";
        Long userId = 999L;

        PasswordResetToken resetToken = createValidResetToken(token, userId);

        when(tokenRepository.findByToken(token))
                .thenReturn(Optional.of(resetToken));
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        InvalidTokenException exception = assertThrows(
                InvalidTokenException.class,
                () -> resetPasswordUseCase.execute(token, newPassword)
        );

        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository, never()).update(any());
    }

    // Helper methods
    private User createTestUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setUuid(UUID.randomUUID());
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("OldPassword123!"));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return user;
    }

    private PasswordResetToken createValidResetToken(String token, Long userId) {
        PasswordResetToken resetToken = new PasswordResetToken(
                userId,
                token,
                LocalDateTime.now(ZoneOffset.UTC).plusHours(1)
        );
        resetToken.setUsed(false);
        return resetToken;
    }
}
