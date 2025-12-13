package com.crudzaso.cityhelp.auth.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(
        name = "RefreshTokenRequest",
        description = "Request to refresh an expired access token using a valid refresh token. Refresh token expires in 7 days."
)
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token cannot be blank")
    @Schema(
            description = "Refresh token obtained from login response (UUID format, expires in 7 days)",
            example = "550e8400-e29b-41d4-a716-446655440000",
            format = "uuid"
    )
    private String refreshToken;
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}