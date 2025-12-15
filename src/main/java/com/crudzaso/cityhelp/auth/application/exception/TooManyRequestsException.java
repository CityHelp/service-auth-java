package com.crudzaso.cityhelp.auth.application.exception;

/**
 * Exception thrown when a user exceeds the rate limit for an operation.
 * Used to indicate that too many requests have been made within a time window.
 *
 * HTTP Status: 429 Too Many Requests
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
public class TooManyRequestsException extends RuntimeException {

    /**
     * Constructs a new TooManyRequestsException with the specified detail message.
     *
     * @param message the detail message explaining the rate limit violation
     */
    public TooManyRequestsException(String message) {
        super(message);
    }

    /**
     * Constructs a new TooManyRequestsException with the specified detail message and cause.
     *
     * @param message the detail message explaining the rate limit violation
     * @param cause the cause of the exception
     */
    public TooManyRequestsException(String message, Throwable cause) {
        super(message, cause);
    }
}
