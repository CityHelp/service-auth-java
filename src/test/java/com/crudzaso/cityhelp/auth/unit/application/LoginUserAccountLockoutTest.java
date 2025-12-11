package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.LoginUserUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginUserUseCase - Account Lockout Tests")
class LoginUserAccountLockoutTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LoginUserUseCase loginUserUseCase;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "correctPassword123");
        testUser.setId(1L);
        testUser.setUuid(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);
        testUser.setFailedLoginAttempts(0);
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts")
    void shouldLockAccount_After5FailedAttempts() {
        // Given - simulate 5 failed attempts
        String email = "john@example.com";
        String wrongPassword = "wrongPassword";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);
        when(userRepository.update(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        // When - fail 5 times
        for (int i = 0; i < 5; i++) {
            assertThrows(
                InvalidCredentialsException.class,
                () -> loginUserUseCase.execute(email, wrongPassword)
            );
        }

        // Then - verify account is locked
        verify(userRepository, times(5)).update(argThat(user ->
            user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() >= 5
        ));
    }

    @Test
    @DisplayName("Should throw exception when trying to login with locked account")
    void shouldThrowException_WhenAccountIsLocked() {
        // Given - user is locked
        testUser.setFailedLoginAttempts(5);
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(15));

        String email = "john@example.com";
        String password = "anyPassword";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(
            InvalidCredentialsException.class,
            () -> loginUserUseCase.execute(email, password),
            "Should throw exception for locked account"
        );
    }

    @Test
    @DisplayName("Should reset failed attempts counter on successful login")
    void shouldResetFailedAttempts_OnSuccessfulLogin() {
        // Given
        testUser.setFailedLoginAttempts(3); // Already had 3 failed attempts

        String email = "john@example.com";
        String correctPassword = "correctPassword123";

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(correctPassword, testUser.getPassword())).thenReturn(true);
        when(userRepository.update(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return user;
        });

        // When
        User result = loginUserUseCase.execute(email, correctPassword);

        // Then
        assertTrue(result.getFailedLoginAttempts() == 0, "Failed attempts should be reset to 0");
    }
}
