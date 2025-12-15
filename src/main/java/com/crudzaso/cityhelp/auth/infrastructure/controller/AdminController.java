package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.infrastructure.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Admin management endpoints")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(
            summary = "Unlock user account",
            description = "Admin endpoint to unlock a user account that has been locked due to too many failed login attempts. " +
                    "Resets the failed login attempts counter and removes any temporary lock. " +
                    "Requires ADMIN role."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User account unlocked successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user does not have ADMIN role"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<AuthResponse> unlockUser(
            @Parameter(
                    name = "userId",
                    description = "User ID to unlock",
                    required = true,
                    example = "1"
            )
            @PathVariable Long userId
    ) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.error("Usuario no encontrado"));
            }

            User user = userOpt.get();

            // Unlock the user
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastFailedLoginAttempt(null);

            userRepository.update(user);

            return ResponseEntity.ok(AuthResponse.success(
                "Usuario desbloqueado exitosamente"
            ));

        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Error al desbloquear usuario: " + e.getMessage()));
        }
    }

    @Operation(
            summary = "Get user lock status",
            description = "Admin endpoint to check if a user account is locked and view failed login attempts. " +
                    "Returns current lock status, number of failed attempts, and unlock time if applicable. " +
                    "Requires ADMIN role."
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "User lock status retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "User not found"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - invalid or missing JWT token"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - user does not have ADMIN role"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping("/users/{userId}/lock-status")
    public ResponseEntity<AuthResponse> getUserLockStatus(
            @Parameter(
                    name = "userId",
                    description = "User ID to check lock status",
                    required = true,
                    example = "1"
            )
            @PathVariable Long userId
    ) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(AuthResponse.error("Usuario no encontrado"));
            }

            User user = userOpt.get();

            String message = "Usuario: " + user.getEmail() +
                "\nIntentos fallidos: " + user.getFailedLoginAttempts() +
                "\nBloqueado: " + (user.isLocked() ? "Sí" : "No");

            if (user.isLocked()) {
                message += "\nDesbloqueado en: " + user.getLockedUntil();
            }

            return ResponseEntity.ok(AuthResponse.success(message));

        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(AuthResponse.error("Error al obtener estado: " + e.getMessage()));
        }
    }
}
