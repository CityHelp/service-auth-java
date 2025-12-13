package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO for authentication endpoint.
 * Follows English naming for technical code with validation.
 */
@Schema(
        name = "LoginRequest",
        description = "User login request with email and password authentication. Returns JWT access token and refresh token."
)
public class LoginRequest {

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    @Size(max = 255, message = "Email no debe exceder 255 caracteres")
    @JsonProperty("email")
    @Schema(
            description = "User's email address for authentication",
            example = "juan.camilo@example.com",
            maxLength = 255,
            format = "email"
    )
    private String email;

    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 8, max = 128, message = "Contraseña debe tener entre 8 y 128 caracteres")
    @JsonProperty("password")
    @Schema(
            description = "User's password",
            example = "SecurePass123!",
            minLength = 8,
            maxLength = 128
    )
    private String password;

    @JsonProperty("remember_me")
    @Schema(
            description = "Whether to extend refresh token expiration (optional, defaults to false)",
            example = "false",
            defaultValue = "false"
    )
    private boolean rememberMe;

    // Default constructor
    public LoginRequest() {
        this.rememberMe = false;
    }

    // Constructor for testing
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
        this.rememberMe = false;
    }

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", rememberMe=" + rememberMe +
                '}';
    }
}