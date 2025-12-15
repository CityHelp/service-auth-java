package com.crudzaso.cityhelp.auth.unit.infrastructure.security;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.infrastructure.security.CustomUserDetailsService;
import com.crudzaso.cityhelp.auth.unit.infrastructure.InfrastructureUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for CustomUserDetailsService.
 *
 * Tests user loading from database, status validation, and Spring Security
 * UserDetails conversion for authentication.
 *
 * <p>This test class covers:</p>
 * <ul>
 *   <li>Happy path: User loading by email, successful authentication</li>
 *   <li>Happy path: User loading by username (email), proper authorities</li>
 *   <li>Error cases: User not found, inactive user, suspended user</li>
 *   <li>Security: Email verification required, account locked status</li>
 *   <li>Edge cases: Case-insensitive email lookup, null password (OAuth users)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService - User Authentication")
class CustomUserDetailsServiceTest extends InfrastructureUnitTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService userDetailsService;

    private static final String USER_EMAIL = "test@example.com";
    private static final String USER_PASSWORD = "encodedPassword123";

    @BeforeEach
    @Override
    protected void setUp() {
        super.setUp();
        userDetailsService = new CustomUserDetailsService(userRepository);
    }

    @Test
    @DisplayName("should load active user by email successfully")
    void shouldLoadActiveUser_ByEmail_Successfully() {
        // Arrange
        User activeUser = createActiveUser(USER_EMAIL);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(activeUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(USER_EMAIL);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_EMAIL);
        assertThat(userDetails.getPassword()).isEqualTo(USER_PASSWORD);
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();

        // Verify authorities
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should load admin user with correct authorities")
    void shouldLoadAdminUser_WithCorrectAuthorities() {
        // Arrange
        User adminUser = createAdminUser(USER_EMAIL);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(adminUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(USER_EMAIL);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_EMAIL);

        // Verify admin authorities
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should handle case-insensitive email lookup")
    void shouldHandleCaseInsensitiveEmailLookup() {
        // Arrange
        String uppercaseEmail = USER_EMAIL.toUpperCase();
        User activeUser = createActiveUser(USER_EMAIL);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(activeUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(uppercaseEmail);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_EMAIL);

        // Verify repository was called with uppercase email
        verify(userRepository, times(1)).findByEmailIgnoreCase(uppercaseEmail);
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user is not found")
    void shouldThrowUsernameNotFoundException_WhenUserIsNotFound() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(USER_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado con email: " + USER_EMAIL);

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user needs email verification")
    void shouldThrowUsernameNotFoundException_WhenUserNeedsEmailVerification() {
        // Arrange
        User pendingUser = createPendingVerificationUser(USER_EMAIL);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(pendingUser));

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(USER_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuario pendiente de verificación de email: " + USER_EMAIL);

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should throw UsernameNotFoundException when user is suspended")
    void shouldThrowUsernameNotFoundException_WhenUserIsSuspended() {
        // Arrange
        User suspendedUser = createSuspendedUser(USER_EMAIL);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(suspendedUser));

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(USER_EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Cuenta de usuario no activa: " + USER_EMAIL);

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should handle OAuth user with null password")
    void shouldHandleOAuthUser_WithNullPassword() {
        // Arrange
        User oauthUser = createOAuthUser(USER_EMAIL);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(oauthUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(USER_EMAIL);

        // Assert
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(USER_EMAIL);
        assertThat(userDetails.getPassword()).isEqualTo(""); // OAuth users have empty string password
        assertThat(userDetails.isEnabled()).isTrue(); // OAuth users are verified by provider

        // Verify authorities
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should handle database exceptions gracefully")
    void shouldHandleDatabaseExceptions_Gracefully() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(anyString()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(USER_EMAIL))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(USER_EMAIL);
    }

    @Test
    @DisplayName("should handle empty username string")
    void shouldHandleEmptyUsernameString() {
        // Arrange
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(""))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado con email: ");

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase("");
    }

    @Test
    @DisplayName("should handle whitespace-only username")
    void shouldHandleWhitespaceOnlyUsername() {
        // Arrange
        String whitespaceUsername = "   ";
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(whitespaceUsername))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("Usuario no encontrado con email:    ");

        // Verify repository interaction
        verify(userRepository, times(1)).findByEmailIgnoreCase(whitespaceUsername);
    }

    // Helper methods for creating test users

    private User createActiveUser(String email) {
        User user = new User();
        user.setId(12345L);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(USER_PASSWORD);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User createAdminUser(String email) {
        User user = new User();
        user.setId(12346L);
        user.setFirstName("Admin");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(USER_PASSWORD);
        user.setRole(UserRole.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true);
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User createPendingVerificationUser(String email) {
        User user = new User();
        user.setId(12347L);
        user.setFirstName("Pending");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(USER_PASSWORD);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setIsVerified(false);
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User createSuspendedUser(String email) {
        User user = new User();
        user.setId(12348L);
        user.setFirstName("Suspended");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(USER_PASSWORD);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.SUSPENDED);
        user.setIsVerified(true);
        user.setOAuthProvider(OAuthProvider.LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private User createOAuthUser(String email) {
        User user = new User();
        user.setId(12349L);
        user.setFirstName("OAuth");
        user.setLastName("User");
        user.setEmail(email);
        user.setPassword(null); // OAuth users have null password
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsVerified(true); // OAuth users are verified by provider
        user.setOAuthProvider(OAuthProvider.GOOGLE);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}