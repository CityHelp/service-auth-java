package com.crudzaso.cityhelp.auth.integration.repository;

import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
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
 * Integration tests for EmailVerificationRepository using Testcontainers with PostgreSQL.
 *
 * Tests all repository methods including:
 * - CRUD operations for verification codes
 * - User-specific verification operations
 * - Code status management and cleanup
 * - Edge cases and validation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class EmailVerificationRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User secondUser;
    private EmailVerificationCode activeCode;
    private EmailVerificationCode expiredCode;
    private EmailVerificationCode usedCode;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = createTestUser("user1@example.com", "User", "One");
        secondUser = createTestUser("user2@example.com", "User", "Two");

        testUser = userRepository.save(testUser);
        secondUser = userRepository.save(secondUser);

        // Create verification codes
        activeCode = createVerificationCode(testUser.getId(), "123456", false, LocalDateTime.now().plusMinutes(15));
        expiredCode = createVerificationCode(testUser.getId(), "654321", false, LocalDateTime.now().minusMinutes(5));
        usedCode = createVerificationCode(secondUser.getId(), "111111", true, LocalDateTime.now().plusMinutes(10));

        activeCode = emailVerificationRepository.save(activeCode);
        expiredCode = emailVerificationRepository.save(expiredCode);
        usedCode = emailVerificationRepository.save(usedCode);
    }

    @AfterEach
    void tearDown() {
        // Clean up database after each test
        cleanupTestData();
    }

    // ========== CRUD Operations ==========

    @Test
    @DisplayName("Should save verification code with all required fields")
    void shouldSaveVerificationCode_WithValidData() {
        // Arrange
        EmailVerificationCode newCode = createVerificationCode(
            testUser.getId(),
            "555555",
            false,
            LocalDateTime.now().plusMinutes(30)
        );

        // Act
        EmailVerificationCode savedCode = emailVerificationRepository.save(newCode);

        // Assert
        assertThat(savedCode.getId()).isNotNull();
        assertThat(savedCode.getUserId()).isEqualTo(testUser.getId());
        assertThat(savedCode.getCode()).isEqualTo("555555");
        assertThat(savedCode.isUsed()).isFalse();
        assertThat(savedCode.getExpiresAt()).isNotNull();
        assertThat(savedCode.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find verification code by ID")
    void shouldFindVerificationCodeById_WhenCodeExists() {
        // Act
        Optional<EmailVerificationCode> found = emailVerificationRepository.findById(activeCode.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("123456");
        assertThat(found.get().getUserId()).isEqualTo(testUser.getId());
        assertThat(found.get().isUsed()).isFalse();
    }

    // ========== User-specific Operations ==========

    @Test
    @DisplayName("Should find latest verification code for user")
    void shouldFindLatestVerificationCodeForUser() {
        // Arrange - create a newer code
        EmailVerificationCode newerCode = createVerificationCode(
            testUser.getId(),
            "777777",
            false,
            LocalDateTime.now().plusMinutes(25)
        );
        emailVerificationRepository.save(newerCode);

        // Act
        Optional<EmailVerificationCode> latest = emailVerificationRepository.findLatestByUserId(testUser.getId());

        // Assert
        assertThat(latest).isPresent();
        assertThat(latest.get().getCode()).isEqualTo("777777"); // Should be the newest
    }

    @Test
    @DisplayName("Should find all verification codes for a specific user")
    void shouldFindAllVerificationCodesForUser() {
        // Act
        List<EmailVerificationCode> userCodes = emailVerificationRepository.findByUserId(testUser.getId());

        // Assert
        assertThat(userCodes).hasSize(2); // activeCode + expiredCode
        assertThat(userCodes)
            .extracting(EmailVerificationCode::getUserId)
            .containsOnly(testUser.getId());
    }

    @Test
    @DisplayName("Should return empty list when user has no verification codes")
    void shouldReturnEmpty_WhenUserHasNoVerificationCodes() {
        // Arrange - create user with no codes
        User userWithoutCodes = createTestUser("nocodes@example.com", "No", "Codes");
        userWithoutCodes = userRepository.save(userWithoutCodes);

        // Act
        List<EmailVerificationCode> userCodes = emailVerificationRepository.findByUserId(userWithoutCodes.getId());

        // Assert
        assertThat(userCodes).isEmpty();
    }

    // ========== Status-based Operations ==========

    @Test
    @DisplayName("Should find all active verification codes")
    void shouldFindAllActiveVerificationCodes() {
        // Act
        List<EmailVerificationCode> activeCodes = emailVerificationRepository.findActive();

        // Assert
        assertThat(activeCodes).hasSize(1);
        assertThat(activeCodes.get(0).getCode()).isEqualTo("123456");
        assertThat(activeCodes.get(0).isUsed()).isFalse();
        assertThat(activeCodes.get(0).getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find all expired verification codes")
    void shouldFindAllExpiredVerificationCodes() {
        // Act
        List<EmailVerificationCode> expiredCodes = emailVerificationRepository.findExpired();

        // Assert
        assertThat(expiredCodes).hasSize(1);
        assertThat(expiredCodes.get(0).getCode()).isEqualTo("654321");
        assertThat(expiredCodes.get(0).getExpiresAt()).isBefore(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should find all used verification codes")
    void shouldFindAllUsedVerificationCodes() {
        // Act
        List<EmailVerificationCode> usedCodes = emailVerificationRepository.findUsed();

        // Assert
        assertThat(usedCodes).hasSize(1);
        assertThat(usedCodes.get(0).getCode()).isEqualTo("111111");
        assertThat(usedCodes.get(0).isUsed()).isTrue();
    }

    // ========== Code Management Operations ==========

    @Test
    @DisplayName("Should mark verification code as used by ID")
    void shouldMarkVerificationCodeAsUsedById() {
        // Act
        emailVerificationRepository.markAsUsedById(activeCode.getId());

        // Assert
        Optional<EmailVerificationCode> updated = emailVerificationRepository.findById(activeCode.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().isUsed()).isTrue();
    }

    // ========== Cleanup Operations ==========

    @Test
    @DisplayName("Should delete all verification codes for a specific user")
    void shouldDeleteAllVerificationCodesForUser() {
        // Act
        emailVerificationRepository.deleteAllByUserId(testUser.getId());

        // Assert
        List<EmailVerificationCode> remainingCodes = emailVerificationRepository.findByUserId(testUser.getId());
        assertThat(remainingCodes).isEmpty();

        // Verify other user's codes are still there
        List<EmailVerificationCode> secondUserCodes = emailVerificationRepository.findByUserId(secondUser.getId());
        assertThat(secondUserCodes).hasSize(1);
    }

    @Test
    @DisplayName("Should delete all expired verification codes")
    void shouldDeleteAllExpiredVerificationCodes() {
        // Arrange - add more expired codes
        EmailVerificationCode expiredCode2 = createVerificationCode(
            testUser.getId(),
            "expired-2",
            false,
            LocalDateTime.now().minusMinutes(10)
        );
        EmailVerificationCode expiredCode3 = createVerificationCode(
            secondUser.getId(),
            "expired-3",
            true,
            LocalDateTime.now().minusHours(1)
        );
        emailVerificationRepository.save(expiredCode2);
        emailVerificationRepository.save(expiredCode3);

        // Act
        int deletedCount = emailVerificationRepository.deleteExpired();

        // Assert
        assertThat(deletedCount).isEqualTo(3); // expiredCode + expiredCode2 + expiredCode3

        // Verify expired codes are deleted
        List<EmailVerificationCode> remainingExpired = emailVerificationRepository.findExpired();
        assertThat(remainingExpired).isEmpty();
    }

    @Test
    @DisplayName("Should delete all used verification codes")
    void shouldDeleteAllUsedVerificationCodes() {
        // Arrange - add more used codes
        EmailVerificationCode usedCode2 = createVerificationCode(
            testUser.getId(),
            "used-2",
            true,
            LocalDateTime.now().plusMinutes(5)
        );
        emailVerificationRepository.save(usedCode2);

        // Act
        int deletedCount = emailVerificationRepository.deleteUsed();

        // Assert
        assertThat(deletedCount).isEqualTo(2); // usedCode + usedCode2

        // Verify used codes are deleted
        List<EmailVerificationCode> remainingUsed = emailVerificationRepository.findUsed();
        assertThat(remainingUsed).isEmpty();
    }

    // ========== Count and Validation Operations ==========

    @Test
    @DisplayName("Should count active verification codes for user")
    void shouldCountActiveVerificationCodesForUser() {
        // Act
        long activeCount = emailVerificationRepository.countActiveByUserId(testUser.getId());

        // Assert
        assertThat(activeCount).isEqualTo(1); // Only activeCode is active
    }

    @Test
    @DisplayName("Should check if user has active verification codes")
    void shouldCheckUserHasActiveVerificationCodes() {
        // Act
        boolean hasActiveCodes = emailVerificationRepository.hasActiveCodes(testUser.getId());

        // Assert
        assertThat(hasActiveCodes).isTrue();

        // Check for user without active codes
        boolean secondUserHasActiveCodes = emailVerificationRepository.hasActiveCodes(secondUser.getId());
        assertThat(secondUserHasActiveCodes).isFalse(); // secondUser only has usedCode
    }

    // ========== Edge Cases and Validation ==========

    @Test
    @DisplayName("Should handle verification codes with proper validation")
    void shouldHandleVerificationCodesWithProperValidation() {
        // Arrange
        EmailVerificationCode code = createVerificationCode(
            testUser.getId(),
            "000000",
            false,
            LocalDateTime.now().plusMinutes(30)
        );

        // Act
        EmailVerificationCode saved = emailVerificationRepository.save(code);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getCode()).hasSize(6); // Should be 6 digits
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Should update verification code entity")
    void shouldUpdateVerificationCodeEntity() {
        // Arrange
        activeCode.setUsed(true);
        activeCode.setExpiresAt(LocalDateTime.now().plusHours(1));

        // Act
        EmailVerificationCode updated = emailVerificationRepository.update(activeCode);

        // Assert
        assertThat(updated.isUsed()).isTrue();
        assertThat(updated.getExpiresAt()).isAfter(activeCode.getCreatedAt());
    }

    // ========== Helper Methods ==========

    /**
     * Create a test email verification code.
     */
    private EmailVerificationCode createVerificationCode(Long userId, String code, boolean used,
                                                        LocalDateTime expiresAt) {
        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUserId(userId);
        verificationCode.setCode(code);
        verificationCode.setUsed(used);
        verificationCode.setExpiresAt(expiresAt);
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setAttempts(0);
        return verificationCode;
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
            // Clean up verification codes
            List<EmailVerificationCode> codes = emailVerificationRepository.findByUserId(testUser.getId());
            for (EmailVerificationCode code : codes) {
                emailVerificationRepository.deleteById(code.getId());
            }

            codes = emailVerificationRepository.findByUserId(secondUser.getId());
            for (EmailVerificationCode code : codes) {
                emailVerificationRepository.deleteById(code.getId());
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

            // Clean up any additional test users
            List<User> allUsers = userRepository.findByRole(UserRole.USER);
            for (User userToDelete : allUsers) {
                if (userToDelete.getEmail().contains("test") ||
                    userToDelete.getEmail().contains("example") ||
                    userToDelete.getEmail().contains("nocodes")) {
                    // Clean up related verification codes first
                    List<EmailVerificationCode> userCodes = emailVerificationRepository.findByUserId(userToDelete.getId());
                    for (EmailVerificationCode code : userCodes) {
                        emailVerificationRepository.deleteById(code.getId());
                    }
                    userRepository.deleteById(userToDelete.getId());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the test
            System.err.println("Error cleaning up test data: " + e.getMessage());
        }
    }
}