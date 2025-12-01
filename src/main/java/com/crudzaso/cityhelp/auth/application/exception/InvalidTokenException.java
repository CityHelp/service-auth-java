package com.crudzaso.cityhelp.auth.application.exception;

/**
 * Exception thrown when a JWT token is invalid or malformed.
 * This is a business rule validation exception.
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
