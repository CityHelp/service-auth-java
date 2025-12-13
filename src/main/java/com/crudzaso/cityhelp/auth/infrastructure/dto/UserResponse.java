package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.crudzaso.cityhelp.auth.domain.model.User;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for user authentication responses.
 * Follows English naming convention for technical code.
 *
 * @param success Indicates if operation was successful
 * @param message Response message for user feedback
 * @param error Optional error message if operation failed
 * @param user Optional user data (for successful operations)
 */
@Schema(
        name = "UserResponse",
        description = "Response containing user information after successful authentication or user profile query"
)
public class UserResponse {
    @Schema(
            description = "Whether the operation was successful",
            example = "true"
    )
    private boolean success;

    @Schema(
            description = "Response message describing the operation result",
            example = "User retrieved successfully"
    )
    private String message;

    @Schema(
            description = "User object containing profile information (only present on successful operations)"
    )
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