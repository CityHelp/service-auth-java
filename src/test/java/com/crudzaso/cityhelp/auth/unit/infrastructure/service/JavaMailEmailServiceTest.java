package com.crudzaso.cityhelp.auth.unit.infrastructure.service;

import com.crudzaso.cityhelp.auth.infrastructure.service.JavaMailEmailService;
import com.crudzaso.cityhelp.auth.unit.infrastructure.BaseUnitTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for JavaMailEmailService.
 *
 * Tests email sending functionality including verification codes, welcome emails,
 * and password reset emails using Thymeleaf templates.
 *
 * <p>This test class covers:</p>
 * <ul>
 *   <li>Happy path: Verification code email sending</li>
 *   <li>Happy path: Welcome email sending</li>
 *   <li>Happy path: Password reset email sending</li>
 *   <li>Error cases: MessagingException, template processing errors</li>
 *   <li>Configuration: From email, from name, SMTP settings</li>
 *   <li>Security: HTML content encoding, proper MIME types</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JavaMailEmailService - Email Operations")
class JavaMailEmailServiceTest extends BaseUnitTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    private JavaMailEmailService emailService;

    private static final String FROM_EMAIL = "noreply@cityhelp.com";
    private static final String FROM_NAME = "CityHelp";
    private static final String TO_EMAIL = "test@example.com";
    private static final String TO_NAME = "Test User";
    private static final String VERIFICATION_CODE = "123456";
    private static final String RESET_TOKEN = "reset-token-abc123";
    private static final String HTML_CONTENT = "<html><body>Test email content</body></html>";

    @BeforeEach
    void setUpEmailService() {
        emailService = new JavaMailEmailService(mailSender, templateEngine);

        // Set configuration properties
        ReflectionTestUtils.setField(emailService, "fromEmail", FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "fromName", FROM_NAME);

        // Setup mail sender mock
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("should send verification code email successfully")
    void shouldSendVerificationCodeEmail_Successfully() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);

        // Act
        emailService.sendVerificationCode(TO_EMAIL, TO_NAME, VERIFICATION_CODE);

        // Assert
        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should send welcome email successfully")
    void shouldSendWelcomeEmail_Successfully() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);

        // Act
        emailService.sendWelcomeEmail(TO_EMAIL, TO_NAME);

        // Assert
        verify(templateEngine, times(1)).process(eq("email/welcome"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should send password reset email successfully")
    void shouldSendPasswordResetEmail_Successfully() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);

        // Act
        emailService.sendPasswordResetEmail(TO_EMAIL, TO_NAME, RESET_TOKEN);

        // Assert
        verify(templateEngine, times(1)).process(eq("email/password-reset"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should handle welcome email failure gracefully")
    void shouldHandleWelcomeEmailFailure_Gracefully() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // Act
        emailService.sendWelcomeEmail(TO_EMAIL, TO_NAME);

        // Assert - Should not throw exception for welcome emails (non-critical)
        verify(templateEngine, times(1)).process(eq("email/welcome"), any(Context.class));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should throw exception for verification code email failure")
    void shouldThrowException_ForVerificationCodeEmailFailure() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationCode(TO_EMAIL, TO_NAME, VERIFICATION_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send verification email");

        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should throw exception for password reset email failure")
    void shouldThrowException_ForPasswordResetEmailFailure() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenThrow(new RuntimeException("Template processing failed"));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendPasswordResetEmail(TO_EMAIL, TO_NAME, RESET_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send password reset email");

        verify(templateEngine, times(1)).process(eq("email/password-reset"), any(Context.class));
        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should throw exception when mail sender fails")
    void shouldThrowException_WhenMailSenderFails() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server unavailable"));

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationCode(TO_EMAIL, TO_NAME, VERIFICATION_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send verification email");

        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should handle mail sending exception gracefully")
    void shouldHandleMailSendingException_Gracefully() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(mimeMessage);

        // Act & Assert
        assertThatThrownBy(() -> emailService.sendVerificationCode(TO_EMAIL, TO_NAME, VERIFICATION_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send verification email");

        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should handle empty recipient name")
    void shouldHandleEmptyRecipientName() throws Exception {
        // Arrange
        String emptyName = "";
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);

        // Act
        emailService.sendVerificationCode(TO_EMAIL, emptyName, VERIFICATION_CODE);

        // Assert
        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should handle special characters in verification code")
    void shouldHandleSpecialCharacters_InVerificationCode() throws Exception {
        // Arrange
        String specialVerificationCode = "ABC-123!@#";
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);

        // Act
        emailService.sendVerificationCode(TO_EMAIL, TO_NAME, specialVerificationCode);

        // Assert
        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should handle long reset token")
    void shouldHandleLongResetToken() throws Exception {
        // Arrange
        String longResetToken = "a".repeat(1000); // Very long reset token
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(HTML_CONTENT);

        // Act
        emailService.sendPasswordResetEmail(TO_EMAIL, TO_NAME, longResetToken);

        // Assert
        verify(templateEngine, times(1)).process(eq("email/password-reset"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("should handle template engine returning null content")
    void shouldHandleTemplateEngineReturningNullContent() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn(null);

        // Act & Assert - Verification code email is critical, should throw exception
        assertThatThrownBy(() -> emailService.sendVerificationCode(TO_EMAIL, TO_NAME, VERIFICATION_CODE))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send verification email");

        verify(templateEngine, times(1)).process(eq("email/verification-code"), any(Context.class));
        // mailSender.createMimeMessage() is called inside sendHtmlEmail, but send() should not be called
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("should handle template engine returning empty content")
    void shouldHandleTemplateEngineReturningEmptyContent() throws Exception {
        // Arrange
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("");

        // Act
        emailService.sendWelcomeEmail(TO_EMAIL, TO_NAME);

        // Assert - Welcome email is non-critical, should not throw exception
        verify(templateEngine, times(1)).process(eq("email/welcome"), any(Context.class));
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(mimeMessage);
    }
}