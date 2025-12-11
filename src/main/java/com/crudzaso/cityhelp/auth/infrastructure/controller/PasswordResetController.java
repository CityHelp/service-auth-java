package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.application.RequestPasswordResetUseCase;
import com.crudzaso.cityhelp.auth.application.ResetPasswordUseCase;
import com.crudzaso.cityhelp.auth.application.ValidateResetTokenUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import com.crudzaso.cityhelp.auth.infrastructure.dto.ForgotPasswordRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.ResetPasswordRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Password Reset", description = "Password reset and recovery endpoints")
public class PasswordResetController {

    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ValidateResetTokenUseCase validateResetTokenUseCase;

    public PasswordResetController(
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            ValidateResetTokenUseCase validateResetTokenUseCase
    ) {
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.validateResetTokenUseCase = validateResetTokenUseCase;
    }

    @Operation(summary = "Request password reset")
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetUseCase.execute(request.getEmail());

        return ResponseEntity.ok(AuthResponse.success(
            "Si el email existe en el sistema, recibirá un enlace para restablecer su contraseña"
        ));
    }

    @Operation(summary = "Reset password with token")
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            resetPasswordUseCase.execute(request.getToken(), request.getNewPassword());

            return ResponseEntity.ok(AuthResponse.success(
                "Contraseña restablecida exitosamente"
            ));
        } catch (InvalidTokenException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Validate reset token")
    @PostMapping("/validate-reset-token")
    public ResponseEntity<AuthResponse> validateResetToken(@RequestParam String token) {
        boolean isValid = validateResetTokenUseCase.execute(token);

        if (isValid) {
            return ResponseEntity.ok(AuthResponse.success("Token válido"));
        } else {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.error("Token inválido o expirado"));
        }
    }
}
