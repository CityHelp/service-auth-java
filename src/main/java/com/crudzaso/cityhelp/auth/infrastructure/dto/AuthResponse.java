package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard API response for authentication operations.
 * Follows English naming for technical code with Spanish user messages.
 */
@Schema(
        name = "AuthResponse",
        description = "Authentication response containing JWT tokens and user information. Access token expires in 24 hours, refresh token in 7 days."
)
public class AuthResponse {

    @JsonProperty("success")
    @Schema(
            description = "Whether the authentication operation was successful",
            example = "true"
    )
    private boolean success;

    @JsonProperty("message")
    @Schema(
            description = "Response message describing the result of the operation",
            example = "User logged in successfully"
    )
    private String message;

    @JsonProperty("access_token")
    @Schema(
            description = "JWT access token (RS256 signed, expires in 24 hours). Include in Authorization header as 'Bearer {token}'",
            example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ...",
            pattern = "^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$"
    )
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(
            description = "Refresh token UUID (expires in 7 days). Use this with /api/auth/refresh endpoint to obtain new access token",
            example = "550e8400-e29b-41d4-a716-446655440000",
            format = "uuid"
    )
    private String refreshToken;

    @JsonProperty("token_type")
    @Schema(
            description = "Token type (always 'Bearer' for JWT tokens)",
            example = "Bearer",
            defaultValue = "Bearer"
    )
    private String tokenType;

    @JsonProperty("expires_in")
    @Schema(
            description = "Access token expiration time in seconds (86400 = 24 hours)",
            example = "86400",
            defaultValue = "86400"
    )
    private long expiresIn;

    @JsonProperty("user")
    @Schema(
            description = "Authenticated user information"
    )
    private UserInfo user;

    // Constructors
    public AuthResponse() {
        this.success = true;
        this.tokenType = "Bearer";
        this.expiresIn = 86400; // 24 hours in seconds
    }

    public AuthResponse(String message) {
        this();
        this.message = message;
    }

    public AuthResponse(String message, String accessToken, String refreshToken, UserInfo user) {
        this(message);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }

    // Static factory methods for common responses
    public static AuthResponse success(String message) {
        return new AuthResponse(message);
    }

    public static AuthResponse success(String message, String accessToken, String refreshToken, UserInfo user) {
        return new AuthResponse(message, accessToken, refreshToken, user);
    }

    public static AuthResponse error(String message) {
        AuthResponse response = new AuthResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    // Getters and setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }

    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    /**
     * User information nested object for auth responses.
     */
    @Schema(
            name = "UserInfo",
            description = "Authenticated user information embedded in authentication response"
    )
    public static class UserInfo {
        @JsonProperty("uuid")
        @Schema(
                description = "User's unique UUID identifier",
                example = "550e8400-e29b-41d4-a716-446655440000",
                format = "uuid"
        )
        private String uuid;

        @JsonProperty("first_name")
        @Schema(
                description = "User's first name",
                example = "Juan"
        )
        private String firstName;

        @JsonProperty("last_name")
        @Schema(
                description = "User's last name",
                example = "Camilo"
        )
        private String lastName;

        @JsonProperty("email")
        @Schema(
                description = "User's email address",
                example = "juan.camilo@example.com",
                format = "email"
        )
        private String email;

        @JsonProperty("role")
        @Schema(
                description = "User's role (USER or ADMIN)",
                example = "USER",
                allowableValues = {"USER", "ADMIN"}
        )
        private String role;

        @JsonProperty("is_verified")
        @Schema(
                description = "Whether the user's email has been verified",
                example = "true"
        )
        private boolean isVerified;

        @JsonProperty("oauth_provider")
        @Schema(
                description = "OAuth provider used for registration (LOCAL for email/password, GOOGLE for OAuth2)",
                example = "LOCAL",
                allowableValues = {"LOCAL", "GOOGLE"}
        )
        private String oauthProvider;

        // Constructors
        public UserInfo() {}

        public UserInfo(String uuid, String firstName, String lastName, String email,
                     String role, boolean isVerified, String oauthProvider) {
            this.uuid = uuid;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.role = role;
            this.isVerified = isVerified;
            this.oauthProvider = oauthProvider;
        }

        // Getters and setters
        public String getUuid() { return uuid; }
        public void setUuid(String uuid) { this.uuid = uuid; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public boolean getIsVerified() { return isVerified; }
        public void setIsVerified(boolean isVerified) { this.isVerified = isVerified; }

        public String getOAuthProvider() { return oauthProvider; }
        public void setOAuthProvider(String oauthProvider) { this.oauthProvider = oauthProvider; }
    }
}