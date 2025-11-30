package com.crudzaso.cityhelp.auth.application;

/**
 * Custom exception for invalid authentication credentials.
 * Follows English naming convention for technical code.
 * 
 * @param message Error message describing the issue
 */
public class InvalidCredentialsException extends RuntimeException {
    
    public InvalidCredentialsException(String message) {
        super(message);
    }
}