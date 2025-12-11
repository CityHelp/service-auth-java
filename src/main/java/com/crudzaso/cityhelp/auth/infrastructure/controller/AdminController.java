package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.infrastructure.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Unlock user account")
    @PostMapping("/users/{userId}/unlock")
    public ResponseEntity<AuthResponse> unlockUser(@PathVariable Long userId) {
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

    @Operation(summary = "Get user lock status")
    @GetMapping("/users/{userId}/lock-status")
    public ResponseEntity<AuthResponse> getUserLockStatus(@PathVariable Long userId) {
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
