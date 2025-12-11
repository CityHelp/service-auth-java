package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.ChangePasswordUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.application.exception.UserNotFoundException;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangePasswordUseCase - Test Suite")
class ChangePasswordUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChangePasswordUseCase changePasswordUseCase;

    private User testUser;
    private Long userId = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User("John", "Doe", "john@example.com", "oldPassword123");
        testUser.setId(userId);
        testUser.setUuid(UUID.randomUUID());
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setRole(UserRole.USER);
        testUser.setIsVerified(true);
    }

    @Test
    @DisplayName("Should change password successfully with correct current password")
    void shouldChangePassword_WithCorrectCurrentPassword() {
        // Given
        String currentPassword = "oldPassword123";
        String newPassword = "newPassword123";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("encoded-new-password");
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        // When
        changePasswordUseCase.execute(userId, currentPassword, newPassword);

        // Then
        verify(userRepository).update(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(
            UserNotFoundException.class,
            () -> changePasswordUseCase.execute(userId, "currentPassword", "newPassword")
        );
    }

    @Test
    @DisplayName("Should throw exception when current password is incorrect")
    void shouldThrowException_WhenCurrentPasswordIsIncorrect() {
        // Given
        String wrongPassword = "wrongPassword123";
        String newPassword = "newPassword123";

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(wrongPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        assertThrows(
            InvalidCredentialsException.class,
            () -> changePasswordUseCase.execute(userId, wrongPassword, newPassword)
        );

        // Verify update was NOT called
        verify(userRepository, never()).update(any(User.class));
    }
}
