package com.crudzaso.cityhelp.auth.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Email verification request DTO.
 * Follows English naming for technical code with validation.
 */
public class VerifyEmailRequest {

    @NotBlank(message = "Email es requerido")
    @Email(message = "Email debe ser válido")
    @Size(max = 255, message = "Email no debe exceder 255 caracteres")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Código de verificación es requerido")
    @Pattern(regexp = "\\d{6}", message = "Código debe tener exactamente 6 dígitos")
    @Size(min = 6, max = 6, message = "Código debe tener 6 dígitos")
    @JsonProperty("verification_code")
    private String verificationCode;

    // Default constructor
    public VerifyEmailRequest() {}

    // Constructor for testing
    public VerifyEmailRequest(String email, String verificationCode) {
        this.email = email;
        this.verificationCode = verificationCode;
    }

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    @Override
    public String toString() {
        return "VerifyEmailRequest{" +
                "email='" + email + '\'' +
                ", verificationCode='" + (verificationCode != null ? verificationCode.substring(0, 2) + "****" : "null") + '\'' +
                '}';
    }
}