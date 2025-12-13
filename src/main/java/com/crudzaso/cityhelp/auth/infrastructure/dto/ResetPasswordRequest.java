package com.crudzaso.cityhelp.auth.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ResetPasswordRequest",
        description = "Request to reset user password using a valid reset token. Password must be strong with uppercase, lowercase, digit, and special character."
)
public class ResetPasswordRequest {

    @NotBlank(message = "El token es requerido")
    @Schema(
            description = "Password reset token from the reset email link",
            example = "550e8400-e29b-41d4-a716-446655440000",
            format = "uuid"
    )
    private String token;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{8,128}$",
            message = "Password must contain uppercase, lowercase, digit, and special character")
    @Schema(
            description = "New password (minimum 8 characters with uppercase, lowercase, digit, and special character)",
            example = "NewSecurePass123!",
            minLength = 8,
            maxLength = 128,
            pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{8,128}$"
    )
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
