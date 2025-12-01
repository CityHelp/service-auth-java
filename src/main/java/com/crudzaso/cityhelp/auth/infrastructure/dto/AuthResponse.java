package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard API response for authentication operations.
 * Follows English naming for technical code with Spanish user messages.
 */
public class AuthResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("user")
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
    public static class UserInfo {
        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("first_name")
        private String firstName;

        @JsonProperty("last_name")
        private String lastName;

        @JsonProperty("email")
        private String email;

        @JsonProperty("role")
        private String role;

        @JsonProperty("is_verified")
        private boolean isVerified;

        @JsonProperty("oauth_provider")
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