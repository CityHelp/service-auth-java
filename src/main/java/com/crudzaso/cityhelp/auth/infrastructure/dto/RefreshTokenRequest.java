package com.crudzaso.cityhelp.auth.infrastructure.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for token refresh requests.
 * Follows English naming convention for technical code.
 * 
 * Business Rules:
 * - Token must be valid and not expired
 * - User must be authenticated
 * - Supports refresh token rotation
 */
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}