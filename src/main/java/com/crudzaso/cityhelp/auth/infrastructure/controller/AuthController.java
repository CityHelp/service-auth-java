package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.infrastructure.dto.*;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.EmailVerificationCode;
import com.crudzaso.cityhelp.auth.domain.repository.UserRepository;
import com.crudzaso.cityhelp.auth.domain.repository.EmailVerificationRepository;
import com.crudzaso.cityhelp.auth.domain.repository.RefreshTokenRepository;
import com.crudzaso.cityhelp.auth.application.RegisterUserUseCase;
import com.crudzaso.cityhelp.auth.application.LoginUserUseCase;
import com.crudzaso.cityhelp.auth.application.VerifyEmailUseCase;
import com.crudzaso.cityhelp.auth.application.RefreshTokenUseCase;
import com.crudzaso.cityhelp.auth.application.LogoutUserUseCase;
import com.crudzaso.cityhelp.auth.application.exception.UserAlreadyExistsException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidVerificationCodeException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import com.crudzaso.cityhelp.auth.application.exception.ExpiredTokenException;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            UserRepository userRepository,
            EmailVerificationRepository emailVerificationRepository,
            RefreshTokenRepository refreshTokenRepository,
            RegisterUserUseCase registerUserUseCase,
            LoginUserUseCase loginUserUseCase,
            VerifyEmailUseCase verifyEmailUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUserUseCase logoutUserUseCase,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.emailVerificationRepository = emailVerificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.registerUserUseCase = registerUserUseCase;
        this.loginUserUseCase = loginUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.logoutUserUseCase = logoutUserUseCase;
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
     * Extracts user ID from JWT token in Authorization header.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract userId from JWT token
            Long userId = extractUserIdFromAuthHeader(authHeader);

            // Find user in database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new InvalidTokenException("Usuario no encontrado"));

            // Create user info response (without password)
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
                            "Usuario obtenido correctamente",
                            null,
                            null,
                            userInfo
                    ));

        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token inválido: " + e.getMessage()));
        } catch (ExpiredTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token expirado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("No autorizado: " + e.getMessage()));
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
     * Extract user ID from Authorization header.
     * Validates JWT token and extracts userId claim.
     *
     * @param authHeader Authorization header value (Bearer <token>)
     * @return User ID from JWT token
     * @throws InvalidTokenException if token is invalid or missing
     * @throws ExpiredTokenException if token is expired
     */
    private Long extractUserIdFromAuthHeader(String authHeader) {
        // Validate header format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Authorization header inválido o ausente");
        }

        // Extract token (remove "Bearer " prefix)
        String token = authHeader.substring(7);

        // Validate token
        if (!jwtTokenProvider.validateToken(token)) {
            throw new InvalidTokenException("Token JWT inválido");
        }

        // Extract userId from token
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);

        if (userId == null) {
            throw new InvalidTokenException("Token no contiene userId");
        }

        return userId;
    }

    /**
     * Refresh access token using a valid refresh token.
     * Generates a new JWT access token without requiring login.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            // Validate refresh token and get user
            User user = refreshTokenUseCase.execute(request.getRefreshToken());

            // Generate new JWT access token
            String newAccessToken = jwtTokenProvider.generateToken(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            // Generate new refresh token (rotating refresh tokens)
            RefreshToken newRefreshToken = refreshTokenUseCase.generateNewToken(user.getId(), 7);

            return ResponseEntity.ok()
                    .body(AuthResponse.success(
                            "Token renovado correctamente",
                            newAccessToken,
                            newRefreshToken.getToken(),
                            null
                    ));

        } catch (InvalidCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Refresh token inválido o expirado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al renovar token: " + e.getMessage()));
        }
    }

    /**
     * Logout user and revoke all refresh tokens.
     * Requires valid JWT token in Authorization header.
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            // Extract userId from JWT token
            Long userId = extractUserIdFromAuthHeader(authHeader);

            // Revoke all refresh tokens for this user
            boolean success = logoutUserUseCase.execute(userId);

            if (!success) {
                return ResponseEntity.badRequest()
                        .body(AuthResponse.error("Error al cerrar sesión"));
            }

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Sesión cerrada correctamente"));

        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token inválido: " + e.getMessage()));
        } catch (ExpiredTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token expirado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al cerrar sesión: " + e.getMessage()));
        }
    }

    /**
     * Delete user account permanently.
     * Requires valid JWT token in Authorization header.
     * Performs cascade deletion of all related entities.
     */
    @DeleteMapping("/delete-account")
    public ResponseEntity<AuthResponse> deleteAccount(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            // 1. Extract userId from JWT token (authentication)
            Long userId = extractUserIdFromAuthHeader(authHeader);

            // 2. Verify user exists
            userRepository.findById(userId)
                    .orElseThrow(() -> new InvalidTokenException("Usuario no encontrado"));

            // 3. Cascade deletion: delete related entities first
            // Delete email verification codes
            emailVerificationRepository.deleteAllByUserId(userId);

            // Revoke all refresh tokens
            refreshTokenRepository.revokeAllByUserId(userId);

            // 4. Delete user account
            userRepository.deleteById(userId);

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Cuenta eliminada permanentemente"));

        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token inválido: " + e.getMessage()));
        } catch (ExpiredTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token expirado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al eliminar cuenta: " + e.getMessage()));
        }
    }
}