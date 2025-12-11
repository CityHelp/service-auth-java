package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO for user registration endpoint.
 * Follows English naming for technical code with validation.
 */
public class RegisterRequest {

    @NotBlank(message = "Nombres son requeridos")
    @Size(min = 2, max = 100, message = "Nombres deben tener entre 2 y 100 caracteres")
    @JsonProperty("first_name")
    private String firstName;

    @NotBlank(message = "Apellidos son requeridos")
    @Size(min = 2, max = 100, message = "Apellidos deben tener entre 2 y 100 caracteres")
    @JsonProperty("last_name")
    private String lastName;

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    @Size(max = 255, message = "Email no debe exceder 255 caracteres")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Contraseña es requerida")
    @Size(min = 8, max = 128, message = "Contraseña debe tener entre 8 y 128 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).{8,128}$",
             message = "Contraseña debe tener mínimo 8 caracteres con mayúscula, minúscula, número y carácter especial")
    @JsonProperty("password")
    private String password;

    @JsonProperty("confirm_password")
    private String confirmPassword;

    // Default constructor
    public RegisterRequest() {}

    // Constructor for testing
    public RegisterRequest(String firstName, String lastName, String email, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    // Getters and setters
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", rememberMe=" + (password != null ? "[PROTECTED]" : "null") +
                '}';
    }
}