package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.crudzaso.cityhelp.auth.domain.model.User;

/**
 * Data Transfer Object for user authentication responses.
 * Follows English naming convention for technical code.
 *
 * @param success Indicates if operation was successful
 * @param message Response message for user feedback
 * @param error Optional error message if operation failed
 * @param user Optional user data (for successful operations)
 */
public class UserResponse {
    private boolean success;
    private String message;
    private User user;

    // Constructor for successful operations
    public UserResponse(boolean success, String message, User user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    // Constructor for failed operations
    public UserResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.user = null;
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }
}