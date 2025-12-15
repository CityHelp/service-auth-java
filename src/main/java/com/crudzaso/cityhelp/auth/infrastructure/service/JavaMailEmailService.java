package com.crudzaso.cityhelp.auth.infrastructure.service;

import com.crudzaso.cityhelp.auth.application.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * JavaMail implementation of EmailService.
 * This is part of the Infrastructure Layer in Clean Architecture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JavaMailEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${cityhelp.email.from}")
    private String fromEmail;

    @Value("${cityhelp.email.from-name}")
    private String fromName;

    @Override
    public void sendVerificationCode(String toEmail, String toName, String verificationCode) {
        log.info("Sending verification code to email: {}", toEmail);

        try {
            Context context = new Context();
            context.setVariable("name", toName);
            context.setVariable("code", verificationCode);
            context.setVariable("expirationMinutes", 15);

            String htmlContent = templateEngine.process("email/verification-code", context);

            sendHtmlEmail(toEmail, "Verifica tu cuenta de CityHelp", htmlContent);

            log.info("Verification code email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification code email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String toName) {
        log.info("Sending welcome email to: {}", toEmail);

        try {
            Context context = new Context();
            context.setVariable("name", toName);

            String htmlContent = templateEngine.process("email/welcome", context);

            sendHtmlEmail(toEmail, "¡Bienvenido a CityHelp!", htmlContent);

            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Non-critical: don't throw exception for welcome emails
        }
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String toName, String resetToken) {
        log.info("Sending password reset email to: {}", toEmail);

        try {
            Context context = new Context();
            context.setVariable("name", toName);
            context.setVariable("resetToken", resetToken);
            context.setVariable("resetUrl", "http://localhost:8001/api/auth/reset-password?token=" + resetToken);

            String htmlContent = templateEngine.process("email/password-reset", context);

            sendHtmlEmail(toEmail, "Restablece tu contraseña de CityHelp", htmlContent);

            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Send HTML email using JavaMailSender
     *
     * @param to recipient email
     * @param subject email subject
     * @param htmlContent HTML content
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}", to, e);
            throw new RuntimeException("Unexpected error sending email", e);
        }
    }
}
