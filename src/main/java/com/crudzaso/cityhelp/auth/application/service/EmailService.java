package com.crudzaso.cityhelp.auth.application.service;

/**
 * Email service interface for sending verification codes and notifications.
 * This is part of the Application Layer in Clean Architecture.
 */
public interface EmailService {

    /**
     * Send verification code to user's email
     *
     * @param toEmail recipient email address
     * @param toName recipient name
     * @param verificationCode 6-digit verification code
     */
    void sendVerificationCode(String toEmail, String toName, String verificationCode);

    /**
     * Send welcome email after successful registration
     *
     * @param toEmail recipient email address
     * @param toName recipient name
     */
    void sendWelcomeEmail(String toEmail, String toName);

    /**
     * Send password reset email
     *
     * @param toEmail recipient email address
     * @param toName recipient name
     * @param resetToken password reset token
     */
    void sendPasswordResetEmail(String toEmail, String toName, String resetToken);
}
