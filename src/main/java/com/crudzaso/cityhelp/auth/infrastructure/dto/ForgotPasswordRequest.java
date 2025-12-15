package com.crudzaso.cityhelp.auth.infrastructure.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(
        name = "ForgotPasswordRequest",
        description = "Request to initiate password reset process. Email must be registered in the system."
)
public class ForgotPasswordRequest {

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe ser válido")
    @Schema(
            description = "Email address associated with the user account",
            example = "juan.camilo@example.com",
            format = "email"
    )
    private String email;

    public ForgotPasswordRequest() {}

    public ForgotPasswordRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
