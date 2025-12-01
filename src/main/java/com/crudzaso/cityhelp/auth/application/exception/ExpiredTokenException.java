package com.crudzaso.cityhelp.auth.application.exception;

/**
 * Exception thrown when a JWT token has expired.
 * This is a business rule validation exception.
 */
public class ExpiredTokenException extends RuntimeException {

    public ExpiredTokenException(String message) {
        super(message);
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
