package com.crudzaso.cityhelp.auth.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
        name = "ChangePasswordRequest",
        description = "Request to change authenticated user's password. Requires current password for verification and new password with strong constraints."
)
public class ChangePasswordRequest {

    @NotBlank(message = "La contraseña actual es requerida")
    @Schema(
            description = "Current password for verification",
            example = "CurrentPass123!"
    )
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es requerida")
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

    public ChangePasswordRequest() {}

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
