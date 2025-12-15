package com.crudzaso.cityhelp.auth.integration.repository;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.integration.BaseIntegrationTest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for UserRepository using Testcontainers with PostgreSQL.
 *
 * Tests all repository methods with real database operations including:
 * - CRUD operations (Create, Read, Update, Delete)
 * - Custom query methods (findByEmail, findByRole, etc.)
 * - Business-specific queries (findActiveUsers, countByStatus)
 * - Edge cases and error scenarios
 */
@Transactional
public class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User oauthUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Create test users for each test
        testUser = createTestUser("john.doe@example.com", "John", "Doe", UserRole.USER);
        oauthUser = createOAuthUser("jane.smith@gmail.com", "Jane", "Smith", OAuthProvider.GOOGLE);
        adminUser = createTestUser("admin@example.com", "Admin", "User", UserRole.ADMIN);

        // Save users to database
        testUser = userRepository.save(testUser);
        oauthUser = userRepository.save(oauthUser);
        adminUser = userRepository.save(adminUser);
    }

    @AfterEach
    void tearDown() {
        // Clean up database after each test
        cleanupTestData();
    }

    // ========== CRUD Operations ==========

    @Test
    @DisplayName("Should save user with all required fields")
    void shouldSaveUser_WithValidData() {
        // Arrange
        User newUser = createTestUser("newuser@example.com", "New", "User", UserRole.USER);

        // Act
        User savedUser = userRepository.save(newUser);

        // Assert
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUuid()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getFirstName()).isEqualTo("New");
        assertThat(savedUser.getLastName()).isEqualTo("User");
        assertThat(savedUser.getRole()).isEqualTo(UserRole.USER);
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(savedUser.getOAuthProvider()).isEqualTo(OAuthProvider.LOCAL);
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getIsVerified()).isFalse();
    }

    @Test
    @DisplayName("Should find user by ID")
    void shouldFindUserById_WhenUserExists() {
        // Act
        Optional<User> found = userRepository.findById(testUser.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(found.get().getUuid()).isEqualTo(testUser.getUuid());
    }

    @Test
    @DisplayName("Should return empty when user ID does not exist")
    void shouldReturnEmpty_WhenUserByIdNotExists() {
        // Act
        Optional<User> found = userRepository.findById(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find user by UUID")
    void shouldFindUserByUuid_WhenUserExists() {
        // Act
        Optional<User> found = userRepository.findByUuid(testUser.getUuid());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo(testUser.getEmail());
        assertThat(found.get().getId()).isEqualTo(testUser.getId());
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUser_WhenValidData() {
        // Arrange
        testUser.setFirstName("Updated John");
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setUpdatedAt(LocalDateTime.now());

        // Act
        User updatedUser = userRepository.update(testUser);

        // Assert
        assertThat(updatedUser.getFirstName()).isEqualTo("Updated John");
        assertThat(updatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(updatedUser.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should delete user by ID")
    void shouldDeleteUserById() {
        // Act
        userRepository.deleteById(testUser.getId());

        // Assert
        Optional<User> found = userRepository.findById(testUser.getId());
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should delete user by UUID")
    void shouldDeleteUserByUuid() {
        // Act
        userRepository.deleteByUuid(testUser.getUuid());

        // Assert
        Optional<User> found = userRepository.findByUuid(testUser.getUuid());
        assertThat(found).isEmpty();
    }

    // ========== Email Operations ==========

    @Test
    @DisplayName("Should find user by email (case sensitive)")
    void shouldFindUserByEmail_WhenExactMatch() {
        // Act
        Optional<User> found = userRepository.findByEmail("john.doe@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should find user by email (case insensitive)")
    void shouldFindUserByEmailIgnoreCase_WhenDifferentCase() {
        // Act
        Optional<User> found = userRepository.findByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckUserExistsByEmail_WhenEmailExists() {
        // Act
        boolean exists = userRepository.existsByEmail("john.doe@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when checking non-existent email")
    void shouldReturnFalse_WhenEmailDoesNotExist() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should check if user exists by email (case insensitive)")
    void shouldCheckUserExistsByEmailIgnoreCase_WhenDifferentCase() {
        // Act
        boolean exists = userRepository.existsByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM");

        // Assert
        assertThat(exists).isTrue();
    }

    // ========== Role and Status Operations ==========

    @Test
    @DisplayName("Should find users by role")
    void shouldFindUsersByRole() {
        // Act
        List<User> users = userRepository.findByRole(UserRole.USER);

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users)
            .extracting(User::getEmail)
            .containsExactlyInAnyOrder("john.doe@example.com", "jane.smith@gmail.com");
    }

    @Test
    @DisplayName("Should find users by status")
    void shouldFindUsersByStatus() {
        // Act
        List<User> users = userRepository.findByStatus(UserStatus.PENDING_VERIFICATION);

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users)
            .allMatch(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("Should find users by OAuth provider")
    void shouldFindUsersByOAuthProvider() {
        // Act
        List<User> users = userRepository.findByOAuthProvider(OAuthProvider.GOOGLE);

        // Assert
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getEmail()).isEqualTo("jane.smith@gmail.com");
        assertThat(users.get(0).getOAuthProvider()).isEqualTo(OAuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("Should find users pending verification")
    void shouldFindUsersPendingVerification() {
        // Act
        List<User> users = userRepository.findByPendingVerification();

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users)
            .allMatch(user -> user.isPendingVerification() && !user.getIsVerified());
    }

    @Test
    @DisplayName("Should find active users")
    void shouldFindActiveUsers() {
        // Arrange - make one user active
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setIsVerified(true);
        userRepository.update(testUser);

        // Act
        List<User> activeUsers = userRepository.findActiveUsers();

        // Assert
        assertThat(activeUsers).hasSize(2); // oauthUser + testUser
        assertThat(activeUsers)
            .allMatch(user -> user.isActive() && user.getIsVerified());
    }

    // ========== Count Operations ==========

    @Test
    @DisplayName("Should count total users")
    void shouldCountTotalUsers() {
        // Act
        long count = userRepository.count();

        // Assert
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should count users by status")
    void shouldCountUsersByStatus() {
        // Act
        long pendingCount = userRepository.countByStatus(UserStatus.PENDING_VERIFICATION);
        long activeCount = userRepository.countByStatus(UserStatus.ACTIVE);

        // Assert
        assertThat(pendingCount).isEqualTo(2);
        assertThat(activeCount).isEqualTo(1); // Only OAuth user is active
    }

    @Test
    @DisplayName("Should count users by role")
    void shouldCountUsersByRole() {
        // Act
        long userCount = userRepository.countByRole(UserRole.USER);
        long adminCount = userRepository.countByRole(UserRole.ADMIN);

        // Assert
        assertThat(userCount).isEqualTo(2);
        assertThat(adminCount).isEqualTo(1);
    }

    // ========== Business Logic Operations ==========

    @Test
    @DisplayName("Should update last login timestamp")
    void shouldUpdateLastLoginAt() {
        // Act
        userRepository.updateLastLoginAt(testUser.getId());

        // Assert
        Optional<User> updated = userRepository.findById(testUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getLastLoginAt()).isNotNull();
        assertThat(updated.get().getLastLoginAt()).isAfter(testUser.getCreatedAt());
    }

    @Test
    @DisplayName("Should mark user as verified")
    void shouldMarkAsVerified() {
        // Act
        userRepository.markAsVerified(testUser.getId());

        // Assert
        Optional<User> updated = userRepository.findById(testUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getIsVerified()).isTrue();
    }

    @Test
    @DisplayName("Should update user status")
    void shouldUpdateStatus() {
        // Act
        userRepository.updateStatus(testUser.getId(), UserStatus.ACTIVE);

        // Assert
        Optional<User> updated = userRepository.findById(testUser.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    // ========== Edge Cases and Validation ==========

    @Test
    @DisplayName("Should find OAuth users without password")
    void shouldHandleOAuthUserWithoutPassword() {
        // Act
        Optional<User> found = userRepository.findByEmail("jane.smith@gmail.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getPassword()).isNull();
        assertThat(found.get().getOAuthProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(found.get().getIsVerified()).isTrue();
        assertThat(found.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should maintain UUID uniqueness")
    void shouldMaintainUuidUniqueness() {
        // Act
        List<User> allUsers = List.of(testUser, oauthUser, adminUser);

        // Assert
        List<UUID> uuids = allUsers.stream()
            .map(User::getUuid)
            .toList();

        assertThat(uuids).hasSize(3);
        assertThat(uuids.stream().distinct()).hasSize(3); // All UUIDs are unique
    }

    // ========== Helper Methods ==========

    /**
     * Create a test user for local authentication.
     */
    private User createTestUser(String email, String firstName, String lastName, UserRole role) {
        User user = new User(firstName, lastName, email, "Password123!");
        user.setUuid(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    /**
     * Create a test user for OAuth authentication.
     */
    private User createOAuthUser(String email, String firstName, String lastName, OAuthProvider provider) {
        User user = new User(firstName, lastName, email, provider);
        user.setUuid(UUID.randomUUID());
        return user;
    }

    /**
     * Clean up test data after each test.
     */
    private void cleanupTestData() {
        try {
            // Clean up all test users by finding them and deleting individually
            List<User> allUsers = List.of(testUser, oauthUser, adminUser);
            for (User user : allUsers) {
                if (user != null && user.getId() != null) {
                    userRepository.deleteById(user.getId());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the test
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }
}