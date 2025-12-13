package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.application.RequestPasswordResetUseCase;
import com.crudzaso.cityhelp.auth.application.ResetPasswordUseCase;
import com.crudzaso.cityhelp.auth.application.ValidateResetTokenUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import com.crudzaso.cityhelp.auth.infrastructure.dto.ForgotPasswordRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.ResetPasswordRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.AuthResponse;
import com.crudzaso.cityhelp.auth.infrastructure.service.MetricsService;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final MetricsService metricsService;

    public PasswordResetController(
            RequestPasswordResetUseCase requestPasswordResetUseCase,
            ResetPasswordUseCase resetPasswordUseCase,
            ValidateResetTokenUseCase validateResetTokenUseCase,
            MetricsService metricsService
    ) {
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.validateResetTokenUseCase = validateResetTokenUseCase;
        this.metricsService = metricsService;
    }

    @Operation(
            summary = "Request password reset",
            description = "Initiates password reset process by sending a reset link to the user's email. " +
                    "For security reasons, this endpoint returns success regardless of whether the email exists " +
                    "to prevent email enumeration attacks. If the email exists, user receives reset link; " +
                    "if not, no email is sent."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset request processed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request format"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        metricsService.recordPasswordResetRequest();
        try {
            requestPasswordResetUseCase.execute(request.getEmail());
            return ResponseEntity.ok(AuthResponse.success(
                "Si el email existe en el sistema, recibirá un enlace para restablecer su contraseña"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(AuthResponse.success(
                "Si el email existe en el sistema, recibirá un enlace para restablecer su contraseña"
            ));
        }
    }

    @Operation(
            summary = "Reset password with token",
            description = "Completes the password reset process by accepting a valid reset token and new password. " +
                    "The reset token is obtained from the password reset email link. " +
                    "Password must be strong: minimum 8 characters with uppercase, lowercase, digit, and special character."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Password reset successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired reset token, or invalid password format"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        Timer.Sample timerSample = metricsService.startPasswordResetTimer();
        try {
            resetPasswordUseCase.execute(request.getToken(), request.getNewPassword());

            // Record successful password reset
            metricsService.recordPasswordResetSuccess();

            return ResponseEntity.ok(AuthResponse.success(
                "Contraseña restablecida exitosamente"
            ));
        } catch (InvalidTokenException e) {
            metricsService.recordPasswordResetFailure();
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.error(e.getMessage()));
        } catch (Exception e) {
            metricsService.recordPasswordResetFailure();
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(AuthResponse.error("Error al restablecer contraseña"));
        } finally {
            metricsService.recordPasswordResetDuration(timerSample);
        }
    }

    @Operation(
            summary = "Validate reset token",
            description = "Validates whether a password reset token is valid and not expired. " +
                    "Used by frontend to check token status before allowing user to proceed with password reset. " +
                    "Reset tokens expire after 24 hours."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token is valid",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token is invalid or expired"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/validate-reset-token")
    public ResponseEntity<AuthResponse> validateResetToken(
            @Parameter(
                    name = "token",
                    description = "Password reset token from the reset email link",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestParam String token
    ) {
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
