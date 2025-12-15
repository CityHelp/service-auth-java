package com.crudzaso.cityhelp.auth.application;

/**
 * Custom exception for invalid email verification codes.
 * Follows English naming convention for technical code.
 * 
 * @param message Error message describing the issue
 */
public class InvalidVerificationCodeException extends RuntimeException {
    
    public InvalidVerificationCodeException(String message) {
        super(message);
    }
}