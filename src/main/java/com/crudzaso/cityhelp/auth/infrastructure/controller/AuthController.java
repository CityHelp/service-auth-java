package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.infrastructure.dto.*;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication REST Controller for CityHelp Auth Service.
 * Follows REST conventions with Spanish user messages.
 * Infrastructure layer controller that handles HTTP requests/responses.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Register a new user in the system.
     * User starts with PENDING_VERIFICATION status.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Check if user already exists
            if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("El email ya está registrado en el sistema"));
            }

            // Create new user (pending verification)
            User newUser = new User(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword()
            );

            User savedUser = userRepository.save(newUser);

            // TODO: Implement email verification code generation and sending
            // This will be implemented in Application Layer

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    savedUser.getUuid().toString(),
                    savedUser.getFirstName(),
                    savedUser.getLastName(),
                    savedUser.getEmail(),
                    savedUser.getRole().name(),
                    savedUser.getIsVerified(),
                    savedUser.getOAuthProvider().name()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AuthResponse.success(
                            "Usuario registrado correctamente. Por favor verifica tu email con el código enviado.",
                            null,
                            null,
                            userInfo
                    ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al registrar usuario: " + e.getMessage()));
        }
    }

    /**
     * Authenticate user with email/username and password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Find user by email or username (for now, only email)
            User user = userRepository.findByEmailIgnoreCase(request.getEmailOrUsername())
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Email o contraseña incorrectos"));
            }

            // TODO: Implement password verification with BCrypt
            // TODO: Implement JWT token generation
            // TODO: Implement refresh token generation
            // These will be implemented in Application Layer with proper security

            if (!user.canLogin()) {
                if (user.needsEmailVerification()) {
                    return ResponseEntity.badRequest()
                            .body(AuthResponse.error("Por favor verifica tu email antes de iniciar sesión"));
                }
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Cuenta no activa. Contacta soporte."));
            }

            // Update last login
            userRepository.updateLastLoginAt(user.getId());

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                    user.getUuid().toString(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getIsVerified(),
                    user.getOAuthProvider().name()
            );

            return ResponseEntity.ok()
                    .body(AuthResponse.success(
                            "Inicio de sesión exitoso",
                            "mock-access-token", // TODO: Implement JWT
                            "mock-refresh-token", // TODO: Implement refresh token
                            userInfo
                    ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al iniciar sesión: " + e.getMessage()));
        }
    }

    /**
     * Verify user email with 6-digit code.
     */
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        try {
            User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Email no encontrado"));
            }

            if (user.getIsVerified()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Email ya verificado"));
            }

            // TODO: Implement email verification code validation
            // TODO: Mark user as verified if code is valid
            // This will be implemented in Application Layer

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Email verificado correctamente"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al verificar email: " + e.getMessage()));
        }
    }

    /**
     * Get current authenticated user information.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser() {
        try {
            // TODO: Implement JWT token extraction and validation
            // This will be implemented with proper Spring Security
            return ResponseEntity.ok()
                    .body(AuthResponse.success("Endpoint implementado con autenticación JWT pendiente"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("No autorizado"));
        }
    }

    /**
     * Resend email verification code.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(@RequestParam String email) {
        try {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Email no encontrado"));
            }

            if (user.getIsVerified()) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Email ya verificado"));
            }

            // TODO: Implement email verification code regeneration and sending
            return ResponseEntity.ok()
                    .body(AuthResponse.success("Código de verificación reenviado"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al reenviar código: " + e.getMessage()));
        }
    }

    /**
     * Delete user account permanently.
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<AuthResponse> deleteAccount(@RequestParam String email) {
        try {
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Email no encontrado"));
            }

            // TODO: Implement proper authentication before deletion
            // TODO: Implement cascade deletion of related entities

            userRepository.deleteByUuid(user.getUuid());

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Cuenta eliminada permanentemente"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al eliminar cuenta: " + e.getMessage()));
        }
    }
}