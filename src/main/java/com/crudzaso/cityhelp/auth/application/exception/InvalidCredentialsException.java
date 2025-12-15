package com.crudzaso.cityhelp.auth.application.exception;

/**
 * Exception thrown when user provides invalid credentials during login.
 * This is a business rule validation exception.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
