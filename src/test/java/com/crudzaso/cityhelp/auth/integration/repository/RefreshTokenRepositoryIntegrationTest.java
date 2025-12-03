package com.crudzaso.cityhelp.auth.integration.repository;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.integration.BaseIntegrationTest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RefreshTokenRepository using Testcontainers with PostgreSQL.
 *
 * Tests all repository methods including:
 * - CRUD operations for refresh tokens
 * - User-specific token operations
 * - Token revocation and cleanup
 * - Edge cases with invalid tokens
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class RefreshTokenRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User secondUser;
    private RefreshToken activeToken;
    private RefreshToken expiredToken;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = createTestUser("user1@example.com", "User", "One");
        secondUser = createTestUser("user2@example.com", "User", "Two");

        testUser = userRepository.save(testUser);
        secondUser = userRepository.save(secondUser);

        // Create refresh tokens
        activeToken = createRefreshToken(testUser.getId(), "active-token-123", false, LocalDateTime.now().plusDays(7));
        expiredToken = createRefreshToken(testUser.getId(), "expired-token-456", true, LocalDateTime.now().minusDays(1));

        activeToken = refreshTokenRepository.save(activeToken);
        expiredToken = refreshTokenRepository.save(expiredToken);
    }

    @AfterEach
    void tearDown() {
        // Clean up database after each test
        cleanupTestData();
    }

    // ========== CRUD Operations ==========

    @Test
    @DisplayName("Should save refresh token with all required fields")
    void shouldSaveRefreshToken_WithValidData() {
        // Arrange
        RefreshToken newToken = createRefreshToken(
            testUser.getId(),
            "new-token-abc",
            false,
            LocalDateTime.now().plusDays(7)
        );

        // Act
        RefreshToken savedToken = refreshTokenRepository.save(newToken);

        // Assert
        assertThat(savedToken.getId()).isNotNull();
        assertThat(savedToken.getToken()).isEqualTo("new-token-abc");
        assertThat(savedToken.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedToken.isRevoked()).isFalse();
        assertThat(savedToken.getExpiresAt()).isNotNull();
        assertThat(savedToken.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find refresh token by token string")
    void shouldFindRefreshTokenByToken_WhenTokenExists() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("active-token-123");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getToken()).isEqualTo("active-token-123");
        assertThat(found.get().getUserId()).isEqualTo(testUser.getId());
        assertThat(found.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Should return empty when token does not exist")
    void shouldReturnEmpty_WhenTokenNotExists() {
        // Act
        Optional<RefreshToken> found = refreshTokenRepository.findByToken("non-existent-token");

        // Assert
        assertThat(found).isEmpty();
    }

    // ========== User-specific Operations ==========

    @Test
    @DisplayName("Should find all tokens for a specific user")
    void shouldFindAllTokensForUser() {
        // Arrange - add another token for the same user
        RefreshToken anotherToken = createRefreshToken(
            testUser.getId(),
            "another-token-def",
            false,
            LocalDateTime.now().plusDays(5)
        );
        refreshTokenRepository.save(anotherToken);

        // Act
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(testUser.getId());

        // Assert
        assertThat(userTokens).hasSize(3); // activeToken + expiredToken + anotherToken
        assertThat(userTokens)
            .extracting(RefreshToken::getUserId)
            .containsOnly(testUser.getId());
    }

    @Test
    @DisplayName("Should find all active (non-revoked) tokens for user")
    void shouldFindAllActiveTokensForUser() {
        // Act
        List<RefreshToken> activeTokens = refreshTokenRepository.findActiveByUserId(testUser.getId());

        // Assert
        assertThat(activeTokens).hasSize(1);
        assertThat(activeTokens.get(0).getToken()).isEqualTo("active-token-123");
        assertThat(activeTokens.get(0).isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Should return empty list when user has no active tokens")
    void shouldReturnEmptyList_WhenUserHasNoActiveTokens() {
        // Act - secondUser has no tokens
        List<RefreshToken> activeTokens = refreshTokenRepository.findActiveByUserId(secondUser.getId());

        // Assert
        assertThat(activeTokens).isEmpty();
    }

    // ========== Token Revocation Operations ==========

    @Test
    @DisplayName("Should revoke all tokens for a specific user")
    void shouldRevokeAllTokensForUser() {
        // Act
        refreshTokenRepository.revokeAllByUserId(testUser.getId());

        // Assert
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(testUser.getId());
        assertThat(userTokens).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("Should revoke a specific token by ID")
    void shouldRevokeSpecificTokenById() {
        // Act
        refreshTokenRepository.revokeById(activeToken.getId());

        // Assert
        Optional<RefreshToken> updatedToken = refreshTokenRepository.findById(activeToken.getId());
        assertThat(updatedToken).isPresent();
        assertThat(updatedToken.get().isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should revoke a specific token by token string")
    void shouldRevokeSpecificTokenByTokenString() {
        // Act
        refreshTokenRepository.revokeByToken("active-token-123");

        // Assert
        Optional<RefreshToken> updatedToken = refreshTokenRepository.findByToken("active-token-123");
        assertThat(updatedToken).isPresent();
        assertThat(updatedToken.get().isRevoked()).isTrue();
    }

    // ========== Token Search Operations ==========

    @Test
    @DisplayName("Should find all expired tokens")
    void shouldFindAllExpiredTokens() {
        // Act
        List<RefreshToken> expiredTokens = refreshTokenRepository.findExpired();

        // Assert
        assertThat(expiredTokens).hasSize(1);
        assertThat(expiredTokens.get(0).getToken()).isEqualTo("expired-token-456");
        assertThat(expiredTokens.get(0).getExpiresAt()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find all revoked tokens")
    void shouldFindAllRevokedTokens() {
        // Act
        List<RefreshToken> revokedTokens = refreshTokenRepository.findRevoked();

        // Assert
        assertThat(revokedTokens).hasSize(1);
        assertThat(revokedTokens.get(0).getToken()).isEqualTo("expired-token-456");
        assertThat(revokedTokens.get(0).isRevoked()).isTrue();
    }

    // ========== Cleanup Operations ==========

    @Test
    @DisplayName("Should delete all expired tokens")
    void shouldDeleteAllExpiredTokens() {
        // Act
        int deletedCount = refreshTokenRepository.deleteExpired();

        // Assert
        assertThat(deletedCount).isEqualTo(1); // expiredToken

        // Verify expired token is deleted
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findByToken("expired-token-456");
        assertThat(deletedToken).isEmpty();

        // Verify non-expired token still exists
        Optional<RefreshToken> activeTokenStillExists = refreshTokenRepository.findByToken("active-token-123");
        assertThat(activeTokenStillExists).isPresent();
    }

    @Test
    @DisplayName("Should delete all tokens for user by ID")
    void shouldDeleteTokensByUserId() {
        // Act
        refreshTokenRepository.deleteAllByUserId(testUser.getId());

        // Assert
        List<RefreshToken> remainingTokens = refreshTokenRepository.findByUserId(testUser.getId());
        assertThat(remainingTokens).isEmpty();

        // Verify other user's tokens are still there (if any)
        List<RefreshToken> secondUserTokens = refreshTokenRepository.findByUserId(secondUser.getId());
        assertThat(secondUserTokens).isEmpty(); // secondUser has no tokens
    }

    // ========== Count Operations ==========

    @Test
    @DisplayName("Should count active tokens for user")
    void shouldCountActiveTokensForUser() {
        // Act
        long activeCount = refreshTokenRepository.countActiveByUserId(testUser.getId());

        // Assert
        assertThat(activeCount).isEqualTo(1); // Only activeToken is active
    }

    @Test
    @DisplayName("Should check if user has active tokens")
    void shouldCheckUserHasActiveTokens() {
        // Act
        boolean hasActiveTokens = refreshTokenRepository.hasActiveTokens(testUser.getId());

        // Assert
        assertThat(hasActiveTokens).isTrue();

        // Check for user with no tokens
        boolean secondUserHasActiveTokens = refreshTokenRepository.hasActiveTokens(secondUser.getId());
        assertThat(secondUserHasActiveTokens).isFalse();
    }

    // ========== Edge Cases and Validation ==========

    @Test
    @DisplayName("Should handle null token string gracefully")
    void shouldHandleNullTokenGracefully() {
        // Act & Assert - should not throw exception
        Optional<RefreshToken> result = refreshTokenRepository.findByToken(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should maintain token uniqueness")
    void shouldMaintainTokenUniqueness() {
        // This test would fail if unique constraint is enforced in database
        // For now, we test basic functionality
        RefreshToken duplicateToken = createRefreshToken(
            secondUser.getId(),
            "active-token-123", // Same token string
            false,
            LocalDateTime.now().plusDays(7)
        );

        // This may throw an exception depending on database constraints
        assertThatThrownBy(() -> refreshTokenRepository.save(duplicateToken))
            .isInstanceOf(Exception.class);
    }

    // ========== Helper Methods ==========

    /**
     * Create a test refresh token.
     */
    private RefreshToken createRefreshToken(Long userId, String token, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setRevoked(revoked);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setCreatedAt(LocalDateTime.now());
        return refreshToken;
    }

    /**
     * Create a test user.
     */
    private User createTestUser(String email, String firstName, String lastName) {
        User user = new User(firstName, lastName, email, "Password123!");
        user.setUuid(UUID.randomUUID());
        return user;
    }

    /**
     * Clean up test data after each test.
     */
    private void cleanupTestData() {
        try {
            // Clean up refresh tokens
            List<RefreshToken> tokens = refreshTokenRepository.findByUserId(testUser.getId());
            for (RefreshToken token : tokens) {
                refreshTokenRepository.deleteById(token.getId());
            }

            tokens = refreshTokenRepository.findByUserId(secondUser.getId());
            for (RefreshToken token : tokens) {
                refreshTokenRepository.deleteById(token.getId());
            }

            // Clean up users
            Optional<User> user = userRepository.findById(testUser.getId());
            if (user.isPresent()) {
                userRepository.deleteById(user.get().getId());
            }

            user = userRepository.findById(secondUser.getId());
            if (user.isPresent()) {
                userRepository.deleteById(user.get().getId());
            }
        } catch (Exception e) {
            // Log error but don't fail the test
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }
}