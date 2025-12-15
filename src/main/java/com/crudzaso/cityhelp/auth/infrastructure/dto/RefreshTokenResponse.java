package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;

/**
 * Data Transfer Object for token refresh responses.
 * Follows English naming convention for technical code.
 *
 * @param success Indicates if operation was successful
 * @param message Response message for user feedback
 * @param token Optional new refresh token
 * @param error Optional error message if operation failed
 */
public class RefreshTokenResponse {

    private boolean success;
    private String message;
    private RefreshToken token;

    // Constructor for successful operations
    public RefreshTokenResponse(boolean success, String message, RefreshToken token) {
        this.success = success;
        this.message = message;
        this.token = token;
    }

    // Constructor for failed operations
    public RefreshTokenResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.token = null;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public RefreshToken getToken() {
        return token;
    }
}