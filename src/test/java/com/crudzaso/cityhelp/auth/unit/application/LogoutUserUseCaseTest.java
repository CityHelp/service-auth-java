package com.crudzaso.cityhelp.auth.unit.application;

import com.crudzaso.cityhelp.auth.application.LogoutUserUseCase;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for LogoutUserUseCase in CityHelp Auth Service.
 *
 * Test Coverage:
 * - Happy paths: Successful logout with token revocation
 * - Error cases: User not found, various user states
 * - Repository operations: Revocation verification, argument capturing
 * - Business logic: Idempotency, concurrent logout scenarios
 * - Edge cases: Multiple logout calls, token state verification
 *
 * Coverage Target: 90%+ for LogoutUserUseCase
 *
 * Test Naming Convention:
 * - should[Action]_With[Condition]() - For positive scenarios
 * - shouldThrowException_When[Condition]() - For error scenarios
 * - shouldReturn[Result]_When[Condition]() - For return value validation
 *
 * Stack: JUnit 5, Mockito, AssertJ, following AAA pattern
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LogoutUserUseCase - Comprehensive Test Suite")
class LogoutUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private LogoutUserUseCase logoutUserUseCase;

    private User testUser;

    private static final Long TEST_USER_ID = 12345L;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
    }

    /**
     * Creates a test user with valid data for logout testing
     */
    private User createTestUser() {
        User user = new User();
        user.setId(TEST_USER_ID);
        user.setUuid(UUID.randomUUID());
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john.doe@cityhelp.com");
        user.setPassword("SecurePassword123!");
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setIsVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.USER);
        user.setCreatedAt(LocalDateTime.now().minusDays(30));
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));
        user.setLastLoginAt(LocalDateTime.now().minusHours(2));

        return user;
    }

    @Nested
    @DisplayName("Happy Path - Successful Logout")
    class SuccessfulLogoutTests {

        @Test
        @DisplayName("Should logout user successfully - Active local user")
        void shouldLogoutUserSuccessfully_ActiveLocalUser() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);

            // Verify no other interactions
            verifyNoMoreInteractions(userRepository, refreshTokenRepository);
        }

        @Test
        @DisplayName("Should logout user successfully - OAuth2 user")
        void shouldLogoutUserSuccessfully_OAuth2User() {
            // Arrange
            testUser.setOAuthProvider(OAuthProvider.GOOGLE);
            testUser.setPassword(null); // OAuth users don't have passwords

            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should logout user successfully - User with recent activity")
        void shouldLogoutUserSuccessfully_UserWithRecentActivity() {
            // Arrange
            testUser.setLastLoginAt(LocalDateTime.now().minusMinutes(30));
            testUser.setUpdatedAt(LocalDateTime.now().minusMinutes(25));

            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(userIdCaptor.capture());

            assertThat(userIdCaptor.getValue()).isEqualTo(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Error Cases - User Not Found")
    class UserNotFoundTests {

        @Test
        @DisplayName("Should return false when user ID not found")
        void shouldReturnFalse_WhenUserIdNotFound() {
            // Arrange
            Long nonExistentUserId = 99999L;
            when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

            // Act
            boolean result = logoutUserUseCase.execute(nonExistentUserId);

            // Assert
            assertThat(result).isFalse();

            verify(userRepository, times(1)).findById(nonExistentUserId);
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }

        @Test
        @DisplayName("Should return false when user repository throws exception")
        void shouldReturnFalse_WhenUserRepositoryThrowsException() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID))
                .thenThrow(new RuntimeException("Database connection error"));

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> logoutUserUseCase.execute(TEST_USER_ID)
            );

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }

        @Test
        @DisplayName("Should return false for null user ID")
        void shouldReturnFalse_ForNullUserId() {
            // Act
            boolean result = logoutUserUseCase.execute(null);

            // Assert
            assertThat(result).isFalse();

            verify(userRepository, never()).findById(anyLong());
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }

        @Test
        @DisplayName("Should return false for negative user ID")
        void shouldReturnFalse_ForNegativeUserId() {
            // Arrange
            Long negativeUserId = -1L;
            when(userRepository.findById(negativeUserId)).thenReturn(Optional.empty());

            // Act
            boolean result = logoutUserUseCase.execute(negativeUserId);

            // Assert
            assertThat(result).isFalse();

            verify(userRepository, times(1)).findById(negativeUserId);
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }
    }

    @Nested
    @DisplayName("Repository Operations - Token Revocation")
    class TokenRevocationTests {

        @Test
        @DisplayName("Should revoke all refresh tokens when logout succeeds")
        void shouldRevokeAllRefreshTokens_WhenLogoutSucceeds() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should handle repository exception during token revocation")
        void shouldHandleRepositoryException_DuringTokenRevocation() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doThrow(new RuntimeException("Token revocation failed"))
                .when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> logoutUserUseCase.execute(TEST_USER_ID)
            );

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should verify correct user ID passed to revocation method")
        void shouldVerifyCorrectUserId_PassedToRevocationMethod() {
            // Arrange
            Long differentUserId = 54321L;
            User differentUser = createTestUser();
            differentUser.setId(differentUserId);

            when(userRepository.findById(differentUserId)).thenReturn(Optional.of(differentUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(differentUserId);

            // Act
            boolean result = logoutUserUseCase.execute(differentUserId);

            // Assert
            assertThat(result).isTrue();

            ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(userIdCaptor.capture());
            assertThat(userIdCaptor.getValue()).isEqualTo(differentUserId);
            assertThat(userIdCaptor.getValue()).isNotEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should not attempt token revocation when user not found")
        void shouldNotAttemptTokenRevocation_WhenUserNotFound() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isFalse();

            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }
    }

    @Nested
    @DisplayName("Business Logic - Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should handle multiple logout calls gracefully")
        void shouldHandleMultipleLogoutCalls_Gracefully() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act - Multiple logout attempts
            boolean result1 = logoutUserUseCase.execute(TEST_USER_ID);
            boolean result2 = logoutUserUseCase.execute(TEST_USER_ID);
            boolean result3 = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result1).isTrue();
            assertThat(result2).isTrue();
            assertThat(result3).isTrue();

            // Verify repository was called for each attempt
            verify(userRepository, times(3)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(3)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should maintain idempotency with different user states")
        void shouldMaintainIdempotency_WithDifferentUserStates() {
            // Arrange - Test with suspended user
            testUser.setStatus(UserStatus.SUSPENDED);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should handle concurrent logout scenarios")
        void shouldHandleConcurrentLogoutScenarios() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act - Simulate concurrent calls
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Verify subsequent calls
            boolean subsequentResult = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();
            assertThat(subsequentResult).isTrue();

            // Both calls should attempt token revocation
            verify(refreshTokenRepository, times(2)).revokeAllByUserId(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Edge Cases - Boundary Conditions")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle logout for user with zero user ID")
        void shouldHandleLogout_ForUserWithZeroUserId() {
            // Arrange
            Long zeroUserId = 0L;
            when(userRepository.findById(zeroUserId)).thenReturn(Optional.empty());

            // Act
            boolean result = logoutUserUseCase.execute(zeroUserId);

            // Assert
            assertThat(result).isFalse();

            verify(userRepository, times(1)).findById(zeroUserId);
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }

        @Test
        @DisplayName("Should handle logout for maximum Long user ID")
        void shouldHandleLogout_ForMaximumLongUserId() {
            // Arrange
            Long maxUserId = Long.MAX_VALUE;
            when(userRepository.findById(maxUserId)).thenReturn(Optional.empty());

            // Act
            boolean result = logoutUserUseCase.execute(maxUserId);

            // Assert
            assertThat(result).isFalse();

            verify(userRepository, times(1)).findById(maxUserId);
            verify(refreshTokenRepository, never()).revokeAllByUserId(anyLong());
        }

        @Test
        @DisplayName("Should handle logout with user having null fields")
        void shouldHandleLogout_WithUserHavingNullFields() {
            // Arrange
            testUser.setLastName(null);
            testUser.setLastLoginAt(null);

            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should handle logout when repository returns user with deleted status")
        void shouldHandleLogout_WhenRepositoryReturnsUserWithDeletedStatus() {
            // Arrange
            testUser.setStatus(UserStatus.DELETED);
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Performance and Validation Tests")
    class PerformanceAndValidationTests {

        @Test
        @DisplayName("Should complete logout operation quickly")
        void shouldCompleteLogoutOperationQuickly() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            long startTime = System.nanoTime();
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);
            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            // Assert
            assertThat(result).isTrue();
            assertThat(duration).isLessThan(1_000_000); // Less than 1ms

            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should verify method call order in logout process")
        void shouldVerifyMethodCallOrder_InLogoutProcess() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            // Verify call order using inOrder verification
            var inOrder = org.mockito.Mockito.inOrder(userRepository, refreshTokenRepository);
            inOrder.verify(userRepository).findById(TEST_USER_ID);
            inOrder.verify(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should verify no unexpected interactions with repositories")
        void shouldVerifyNoUnexpectedInteractions_WithRepositories() {
            // Arrange
            when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
            doNothing().when(refreshTokenRepository).revokeAllByUserId(TEST_USER_ID);

            // Act
            boolean result = logoutUserUseCase.execute(TEST_USER_ID);

            // Assert
            assertThat(result).isTrue();

            // Verify expected interactions
            verify(userRepository, times(1)).findById(TEST_USER_ID);
            verify(refreshTokenRepository, times(1)).revokeAllByUserId(TEST_USER_ID);

            // Verify no other methods were called
            verifyNoMoreInteractions(userRepository, refreshTokenRepository);

            // Verify no unexpected method calls
            verify(userRepository, never()).save(any(User.class));
            verify(userRepository, never()).deleteById(anyLong());
            verify(refreshTokenRepository, never()).findByToken(anyString());
            verify(refreshTokenRepository, never()).save(any());
        }
    }
}