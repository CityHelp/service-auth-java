package com.crudzaso.cityhelp.auth.unit.domain.model;

import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for User domain entity.
 * Tests business logic methods and state transitions.
 *
 * Covers:
 * - User construction (LOCAL and OAuth)
 * - User state validation
 * - Login eligibility checks
 * - Account locking logic
 * - Email verification requirements
 * - Full name generation
 * - Provider type detection
 */
@DisplayName("User Entity Tests")
class UserTest {

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    @DisplayName("Should create LOCAL user with correct initial state when constructed with email and password")
    void shouldCreateLocalUser_WithEmailAndPassword() {
        // Arrange
        String firstName = "Juan";
        String lastName = "Perez";
        String email = "juan@example.com";
        String password = "SecurePassword123!";

        // Act
        User user = new User(firstName, lastName, email, password);

        // Assert
        assertEquals(firstName, user.getFirstName());
        assertEquals(lastName, user.getLastName());
        assertEquals(email, user.getEmail());
        assertEquals(password, user.getPassword());
        assertEquals(OAuthProvider.LOCAL, user.getOAuthProvider());
        assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
        assertEquals(UserRole.USER, user.getRole());
        assertFalse(user.getIsVerified());
        assertNotNull(user.getCreatedAt());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }

    @Test
    @DisplayName("Should create OAuth2 user with ACTIVE status and verified flag when constructed with OAuth provider")
    void shouldCreateOAuthUser_WithProviderAndAutoVerified() {
        // Arrange
        String firstName = "Maria";
        String lastName = "Garcia";
        String email = "maria@gmail.com";
        OAuthProvider provider = OAuthProvider.GOOGLE;

        // Act
        User user = new User(firstName, lastName, email, provider);

        // Assert
        assertEquals(firstName, user.getFirstName());
        assertEquals(lastName, user.getLastName());
        assertEquals(email, user.getEmail());
        assertNull(user.getPassword());
        assertEquals(provider, user.getOAuthProvider());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(UserRole.USER, user.getRole());
        assertTrue(user.getIsVerified());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("Should create empty user with default constructor")
    void shouldCreateEmptyUser_WithDefaultConstructor() {
        // Act
        User user = new User();

        // Assert
        assertNull(user.getId());
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
        assertNull(user.getStatus());
    }

    // ==================== FULL NAME TESTS ====================

    @Test
    @DisplayName("Should return full name concatenated with space when getFullName is called")
    void shouldReturnFullName_WhenFirstAndLastNameSet() {
        // Arrange
        User user = new User("Carlos", "Lopez", "carlos@example.com", "password123");

        // Act
        String fullName = user.getFullName();

        // Assert
        assertEquals("Carlos Lopez", fullName);
    }

    @Test
    @DisplayName("Should return concatenated names even with empty strings")
    void shouldReturnFullName_WithEmptyStrings() {
        // Arrange
        User user = new User();
        user.setFirstName("");
        user.setLastName("");

        // Act
        String fullName = user.getFullName();

        // Assert
        assertEquals(" ", fullName);
    }

    @Test
    @DisplayName("Should return full name with special characters")
    void shouldReturnFullName_WithSpecialCharacters() {
        // Arrange
        User user = new User("José", "García-López", "jose@example.com", "password123");

        // Act
        String fullName = user.getFullName();

        // Assert
        assertEquals("José García-López", fullName);
    }

    // ==================== PROVIDER TYPE DETECTION TESTS ====================

    @Test
    @DisplayName("Should identify LOCAL user correctly when OAuth provider is LOCAL")
    void shouldIdentifyLocalUser_WhenProviderIsLocal() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");

        // Act
        boolean isLocal = user.isLocalUser();
        boolean isOAuth = user.isOAuthUser();

        // Assert
        assertTrue(isLocal);
        assertFalse(isOAuth);
    }

    @Test
    @DisplayName("Should identify OAuth user correctly when OAuth provider is GOOGLE")
    void shouldIdentifyOAuthUser_WhenProviderIsGoogle() {
        // Arrange
        User user = new User("Maria", "Garcia", "maria@gmail.com", OAuthProvider.GOOGLE);

        // Act
        boolean isLocal = user.isLocalUser();
        boolean isOAuth = user.isOAuthUser();

        // Assert
        assertFalse(isLocal);
        assertTrue(isOAuth);
    }

    // ==================== STATUS VALIDATION TESTS ====================

    @Test
    @DisplayName("Should return true when user status is ACTIVE")
    void shouldReturnTrue_WhenUserStatusIsActive() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.ACTIVE);

        // Act
        boolean isActive = user.isActive();

        // Assert
        assertTrue(isActive);
    }

    @Test
    @DisplayName("Should return false when user status is not ACTIVE")
    void shouldReturnFalse_WhenUserStatusIsNotActive() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.PENDING_VERIFICATION);

        // Act
        boolean isActive = user.isActive();

        // Assert
        assertFalse(isActive);
    }

    @Test
    @DisplayName("Should return true when user status is PENDING_VERIFICATION")
    void shouldReturnTrue_WhenUserStatusIsPendingVerification() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");

        // Act
        boolean isPending = user.isPendingVerification();

        // Assert
        assertTrue(isPending);
    }

    @Test
    @DisplayName("Should return false when user status is ACTIVE for isPendingVerification check")
    void shouldReturnFalse_WhenUserStatusIsActivePendingCheck() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.ACTIVE);

        // Act
        boolean isPending = user.isPendingVerification();

        // Assert
        assertFalse(isPending);
    }

    // ==================== ACCOUNT LOCK TESTS ====================

    @Test
    @DisplayName("Should return false when user is not locked (lockedUntil is null)")
    void shouldReturnFalse_WhenUserIsNotLocked() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        assertNull(user.getLockedUntil());

        // Act
        boolean isLocked = user.isLocked();

        // Assert
        assertFalse(isLocked);
    }

    @Test
    @DisplayName("Should return false when lockedUntil is in the past")
    void shouldReturnFalse_WhenLockedUntilIsInPast() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setLockedUntil(LocalDateTime.now().minusHours(1));

        // Act
        boolean isLocked = user.isLocked();

        // Assert
        assertFalse(isLocked);
    }

    @Test
    @DisplayName("Should return true when lockedUntil is in the future")
    void shouldReturnTrue_WhenLockedUntilIsInFuture() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setLockedUntil(LocalDateTime.now().plusHours(1));

        // Act
        boolean isLocked = user.isLocked();

        // Assert
        assertTrue(isLocked);
    }

    @Test
    @DisplayName("Should return true when lockedUntil is exactly 1 second in the future")
    void shouldReturnTrue_WhenLockedUntilIsVeryCloseFuture() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setLockedUntil(LocalDateTime.now().plusSeconds(1));

        // Act
        boolean isLocked = user.isLocked();

        // Assert
        assertTrue(isLocked);
    }

    // ==================== EMAIL VERIFICATION REQUIREMENT TESTS ====================

    @Test
    @DisplayName("Should return true when user is LOCAL and not verified (needs email verification)")
    void shouldReturnTrue_WhenLocalUserNotVerified() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setIsVerified(false);

        // Act
        boolean needsVerification = user.needsEmailVerification();

        // Assert
        assertTrue(needsVerification);
    }

    @Test
    @DisplayName("Should return false when user is LOCAL but already verified")
    void shouldReturnFalse_WhenLocalUserAlreadyVerified() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setIsVerified(true);

        // Act
        boolean needsVerification = user.needsEmailVerification();

        // Assert
        assertFalse(needsVerification);
    }

    @Test
    @DisplayName("Should return false when user is OAuth even if not verified")
    void shouldReturnFalse_WhenOAuthUserNotVerified() {
        // Arrange
        User user = new User("Maria", "Garcia", "maria@gmail.com", OAuthProvider.GOOGLE);
        user.setIsVerified(false);

        // Act
        boolean needsVerification = user.needsEmailVerification();

        // Assert
        assertFalse(needsVerification);
    }

    // ==================== CAN LOGIN TESTS ====================

    @Test
    @DisplayName("Should allow login when user is ACTIVE, verified, and not locked")
    void shouldAllowLogin_WhenAllConditionsMet() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);
        // lockedUntil is null, so not locked

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertTrue(canLogin);
    }

    @Test
    @DisplayName("Should deny login when user status is PENDING_VERIFICATION")
    void shouldDenyLogin_WhenStatusIsPendingVerification() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        // Status is PENDING_VERIFICATION by default
        user.setIsVerified(true);

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("Should deny login when user is not verified")
    void shouldDenyLogin_WhenUserNotVerified() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(false);

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("Should deny login when user account is locked")
    void shouldDenyLogin_WhenUserIsLocked() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);
        user.setLockedUntil(LocalDateTime.now().plusHours(1));

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("Should deny login when user status is DELETED")
    void shouldDenyLogin_WhenUserStatusIsDeleted() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.DELETED);
        user.setIsVerified(true);

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("Should deny login when user status is SUSPENDED")
    void shouldDenyLogin_WhenUserStatusIsSuspended() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        user.setStatus(UserStatus.SUSPENDED);
        user.setIsVerified(true);

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertFalse(canLogin);
    }

    @Test
    @DisplayName("Should allow login for OAuth user even without password")
    void shouldAllowLogin_ForOAuthUserWithoutPassword() {
        // Arrange
        User user = new User("Maria", "Garcia", "maria@gmail.com", OAuthProvider.GOOGLE);
        // Status is ACTIVE, isVerified is true by default for OAuth

        // Act
        boolean canLogin = user.canLogin();

        // Assert
        assertTrue(canLogin);
    }

    // ==================== GETTER/SETTER TESTS ====================

    @Test
    @DisplayName("Should set and get all user fields correctly")
    void shouldSetAndGetAllFields() {
        // Arrange
        User user = new User();
        Long id = 1L;
        UUID uuid = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastTime = now.minusHours(2);

        // Act
        user.setId(id);
        user.setUuid(uuid);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setIsVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setLastLoginAt(pastTime);
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(now.plusHours(1));
        user.setLastFailedLoginAttempt(pastTime);

        // Assert
        assertEquals(id, user.getId());
        assertEquals(uuid, user.getUuid());
        assertEquals("Test", user.getFirstName());
        assertEquals("User", user.getLastName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals(OAuthProvider.LOCAL, user.getOAuthProvider());
        assertTrue(user.getIsVerified());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals(now, user.getCreatedAt());
        assertEquals(now, user.getUpdatedAt());
        assertEquals(pastTime, user.getLastLoginAt());
        assertEquals(3, user.getFailedLoginAttempts());
        assertEquals(now.plusHours(1), user.getLockedUntil());
        assertEquals(pastTime, user.getLastFailedLoginAttempt());
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle null firstName and lastName")
    void shouldHandleNullNames() {
        // Arrange
        User user = new User();
        user.setFirstName(null);
        user.setLastName(null);

        // Act & Assert
        assertNull(user.getFirstName());
        assertNull(user.getLastName());
    }

    @Test
    @DisplayName("Should handle failed login attempts counter")
    void shouldHandleFailedLoginAttempts() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        assertEquals(0, user.getFailedLoginAttempts());

        // Act
        user.setFailedLoginAttempts(1);
        user.setFailedLoginAttempts(2);
        user.setFailedLoginAttempts(3);

        // Assert
        assertEquals(3, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should handle null values for timestamps")
    void shouldHandleNullTimestamps() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");

        // Act
        user.setLastLoginAt(null);
        user.setLockedUntil(null);
        user.setLastFailedLoginAttempt(null);

        // Assert
        assertNull(user.getLastLoginAt());
        assertNull(user.getLockedUntil());
        assertNull(user.getLastFailedLoginAttempt());
    }

    @Test
    @DisplayName("Should have toString method that includes key information")
    void shouldHaveToStringRepresentation() {
        // Arrange
        User user = new User("Juan", "Perez", "juan@example.com", "password123");
        UUID uuid = UUID.randomUUID();
        user.setUuid(uuid);

        // Act
        String toString = user.toString();

        // Assert
        assertNotNull(toString);
        assertTrue(toString.contains("User"));
        assertTrue(toString.contains("juan@example.com"));
    }
}
