package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO for authentication endpoint.
 * Follows English naming for technical code with validation.
 */
public class LoginRequest {

    @NotBlank(message = "Email o nombre de usuario es requerido")
    @Email(message = "Email debe ser válido")
    @Size(max = 255, message = "Email no debe exceder 255 caracteres")
    @JsonProperty("email_or_username")
    private String emailOrUsername;

    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 8, max = 128, message = "Contraseña debe tener entre 8 y 128 caracteres")
    @JsonProperty("password")
    private String password;

    @JsonProperty("remember_me")
    private boolean rememberMe;

    // Default constructor
    public LoginRequest() {
        this.rememberMe = false;
    }

    // Constructor for testing
    public LoginRequest(String emailOrUsername, String password) {
        this.emailOrUsername = emailOrUsername;
        this.password = password;
        this.rememberMe = false;
    }

    // Getters and setters
    public String getEmailOrUsername() { return emailOrUsername; }
    public void setEmailOrUsername(String emailOrUsername) { this.emailOrUsername = emailOrUsername; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "emailOrUsername='" + emailOrUsername + '\'' +
                ", rememberMe=" + rememberMe +
                '}';
    }
}