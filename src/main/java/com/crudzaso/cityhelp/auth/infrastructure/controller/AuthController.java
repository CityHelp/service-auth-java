package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.infrastructure.dto.*;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.application.RegisterUserUseCase;
import com.crudzaso.cityhelp.auth.application.LoginUserUseCase;
import com.crudzaso.cityhelp.auth.application.VerifyEmailUseCase;
import com.crudzaso.cityhelp.auth.application.RefreshTokenUseCase;
import com.crudzaso.cityhelp.auth.application.exception.UserAlreadyExistsException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidVerificationCodeException;
import com.crudzaso.cityhelp.auth.infrastructure.security.JwtTokenProvider;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    private final EmailVerificationRepository emailVerificationRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            VerifyEmailUseCase verifyEmailUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Register a new user in the system.
     * User starts with PENDING_VERIFICATION status.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            // Create new user (pending verification)
            User newUser = new User(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword()
            );

            // Execute registration use case (generates verification code automatically)
            User savedUser = registerUserUseCase.execute(newUser);

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
                            "Usuario registrado. Por favor verifica tu email con el código enviado.",
                            null,
                            null,
                            userInfo
                    ));

        } catch (UserAlreadyExistsException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("El email ya está registrado en el sistema"));
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
            // Execute login use case (validates credentials and checks account status)
            User user = loginUserUseCase.execute(
                    request.getEmailOrUsername(),
                    request.getPassword()
            );

            // Generate JWT access token
            String accessToken = jwtTokenProvider.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            // Generate refresh token (7 days expiration)
            RefreshToken refreshToken = refreshTokenUseCase.generateNewToken(user.getId(), 7);

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
                            accessToken,
                            refreshToken.getToken(),
                            userInfo
                    ));

        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Email o contraseña incorrectos"));
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

            // Execute verification use case (validates code and marks user as verified)
            verifyEmailUseCase.execute(user.getId(), request.getVerificationCode());

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Email verificado correctamente"));

        } catch (InvalidVerificationCodeException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Código de verificación inválido o expirado"));
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

            // Generate new verification code
            String verificationCode = generateVerificationCode();

            // Create and save new email verification code
            EmailVerificationCode emailCode = new EmailVerificationCode();
            emailCode.setUserId(user.getId());
            emailCode.setCode(verificationCode);
            emailCode.setExpiresAt(LocalDateTime.now().plusMinutes(15));
            emailCode.setCreatedAt(LocalDateTime.now());
            emailCode.setUsed(false);
            emailCode.setAttempts(0);

            emailVerificationRepository.save(emailCode);

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Código de verificación reenviado"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al reenviar código: " + e.getMessage()));
        }
    }

    /**
     * Generate a 6-digit verification code.
     * Ensures the code is always exactly 6 digits by padding with zeros if necessary.
     *
     * @return 6-digit numeric string
     */
    private String generateVerificationCode() {
        int code = (int) (Math.random() * 1000000);
        return String.format("%06d", code);
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