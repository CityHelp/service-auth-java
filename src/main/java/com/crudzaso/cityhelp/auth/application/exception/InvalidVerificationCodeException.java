package com.crudzaso.cityhelp.auth.application.exception;

/**
 * Exception thrown when email verification code is invalid, expired, or already used.
 * This is a business rule validation exception.
 */
public class InvalidVerificationCodeException extends RuntimeException {

    public InvalidVerificationCodeException(String message) {
        super(message);
    }

    public InvalidVerificationCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
