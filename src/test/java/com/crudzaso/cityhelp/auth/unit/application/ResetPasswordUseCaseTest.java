package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.ResetPasswordUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.PasswordResetToken;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
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

    @BeforeEach
    void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "password123");
        testUser.setId(1L);
        testUser.setUuid(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);

        validToken = new PasswordResetToken(
            testUser.getId(),
            "valid-token-123",
            LocalDateTime.now().plusHours(1)
        );
    }

    @Test
    @DisplayName("Should reset password successfully with valid token")
    void shouldResetPassword_WithValidToken() {
        // Given
        String token = "valid-token-123";
        String newPassword = "newPassword123";

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(validToken));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded-password");
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        // When
        resetPasswordUseCase.execute(token, newPassword);

        // Then
        verify(userRepository).update(any(User.class));
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("Should throw exception when token is invalid")
    void shouldThrowException_WhenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid-token";

        when(tokenRepository.findByToken(invalidToken)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute(invalidToken, "newPassword123")
        );
    }

    @Test
    @DisplayName("Should throw exception when token is expired")
    void shouldThrowException_WhenTokenIsExpired() {
        // Given
        PasswordResetToken expiredToken = new PasswordResetToken(
            testUser.getId(),
            "expired-token",
            LocalDateTime.now().minusHours(1)
        );

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // When & Then
        assertThrows(
            InvalidTokenException.class,
            () -> resetPasswordUseCase.execute("expired-token", "newPassword123")
        );
    }
}
