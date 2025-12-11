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
import com.crudzaso.cityhelp.auth.application.ChangePasswordUseCase;
import com.crudzaso.cityhelp.auth.application.exception.UserAlreadyExistsException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidVerificationCodeException;
import com.crudzaso.cityhelp.auth.application.exception.InvalidTokenException;
import com.crudzaso.cityhelp.auth.application.exception.ExpiredTokenException;
import com.crudzaso.cityhelp.auth.application.exception.TooManyRequestsException;
import com.crudzaso.cityhelp.auth.infrastructure.security.JwtTokenProvider;
import com.crudzaso.cityhelp.auth.infrastructure.security.RateLimited;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "User authentication and account management endpoints")
public class AuthController {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUserUseCase logoutUserUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
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
            ChangePasswordUseCase changePasswordUseCase,
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
        this.changePasswordUseCase = changePasswordUseCase;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Register a new user in the system.
     * User starts with PENDING_VERIFICATION status.
     */
    @Operation(
            summary = "Register new user",
            description = "Creates a new user account with PENDING_VERIFICATION status. Sends a 6-digit verification code to the user's email. Rate limited to 3 attempts per 15 minutes per IP address."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "User successfully registered, verification code sent to email",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data or email already exists",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many registration attempts, rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PostMapping("/register")
    @RateLimited(key = "register", limit = 3, windowSeconds = 900) // 3 intentos por 15 minutos
    public ResponseEntity<AuthResponse> register(
            @Parameter(description = "User registration details including first name, last name, email, and password", required = true)
            @Valid @RequestBody RegisterRequest request) {
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

        } catch (TooManyRequestsException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponse.error(e.getMessage()));
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
    @Operation(
            summary = "User login",
            description = "Authenticates user with email or username and password. Returns JWT access token (24h) and refresh token (7d). Rate limited to 5 attempts per 5 minutes per IP address."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful, returns access and refresh tokens",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid credentials or account not verified",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many login attempts, rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PostMapping("/login")
    @RateLimited(key = "login", limit = 5, windowSeconds = 300) // 5 intentos por 5 minutos
    public ResponseEntity<AuthResponse> login(
            @Parameter(description = "Login credentials with email/username and password", required = true)
            @Valid @RequestBody LoginRequest request) {
        try {
            // Execute login use case (validates credentials and checks account status)
            User user = loginUserUseCase.execute(
                    request.getEmail(),
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

        } catch (TooManyRequestsException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponse.error(e.getMessage()));
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
    @Operation(
            summary = "Verify email address",
            description = "Verifies user email with 6-digit code sent during registration. Changes user status from PENDING_VERIFICATION to ACTIVE. Rate limited to 3 attempts per 5 minutes."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Email successfully verified, account activated",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid or expired verification code, or email already verified",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Too many verification attempts, rate limit exceeded",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PostMapping("/verify-email")
    @RateLimited(key = "verify-email", limit = 3, windowSeconds = 300) // 3 intentos por 5 minutos
    public ResponseEntity<AuthResponse> verifyEmail(
            @Parameter(description = "Email verification request with email and 6-digit code", required = true)
            @Valid @RequestBody VerifyEmailRequest request) {
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

        } catch (TooManyRequestsException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(AuthResponse.error(e.getMessage()));
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
    @Operation(
            summary = "Get current user",
            description = "Returns information about the currently authenticated user. Requires valid JWT token in Authorization header.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User information retrieved successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired token, authentication required",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(
            @Parameter(description = "JWT token in format 'Bearer {token}'", required = true)
            @RequestHeader("Authorization") String authHeader) {
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
    @Operation(
            summary = "Resend verification code",
            description = "Generates and sends a new 6-digit verification code to the user's email. Only works for unverified accounts."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "New verification code sent successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Email not found or already verified",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(
            @Parameter(description = "User email address", required = true)
            @RequestParam String email) {
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
    @Operation(
            summary = "Refresh access token",
            description = "Generates a new JWT access token using a valid refresh token. Returns new access token (24h) and refresh token (7d). Implements rotating refresh tokens for security."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully, returns new tokens",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad request or token refresh failed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Parameter(description = "Refresh token request with valid refresh token", required = true)
            @Valid @RequestBody RefreshTokenRequest request) {
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
    @Operation(
            summary = "User logout",
            description = "Logs out the current user and revokes all their refresh tokens. Requires valid JWT token in Authorization header.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Logout successful, all refresh tokens revoked",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired token, authentication required",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Logout operation failed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(
            @Parameter(description = "JWT token in format 'Bearer {token}'", required = true)
            @RequestHeader("Authorization") String authHeader) {
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
    @Operation(
            summary = "Delete user account",
            description = "Permanently deletes the user account and all related data (verification codes, refresh tokens). This action cannot be undone. Requires valid JWT token.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Account deleted successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired token, authentication required",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Account deletion failed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @DeleteMapping("/delete-account")
    public ResponseEntity<AuthResponse> deleteAccount(
            @Parameter(description = "JWT token in format 'Bearer {token}'", required = true)
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

            // Delete all refresh tokens (physical deletion, not revoke)
            refreshTokenRepository.deleteAllByUserId(userId);

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

    /**
     * Change user password.
     * Requires valid JWT token in Authorization header.
     * Validates current password before allowing change.
     */
    @Operation(
            summary = "Change password",
            description = "Changes the password for the authenticated user. Requires current password verification. Requires valid JWT token.",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Password changed successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired token, authentication required",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Password change failed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            )
    })
    @PutMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
            @Parameter(description = "JWT token in format 'Bearer {token}'", required = true)
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            // Extract userId from JWT token
            Long userId = extractUserIdFromAuthHeader(authHeader);

            // Execute change password use case
            changePasswordUseCase.execute(userId, request.getCurrentPassword(), request.getNewPassword());

            return ResponseEntity.ok()
                    .body(AuthResponse.success("Contraseña cambiada exitosamente"));

        } catch (InvalidCredentialsException e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error(e.getMessage()));
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token inválido: " + e.getMessage()));
        } catch (ExpiredTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.error("Token expirado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AuthResponse.error("Error al cambiar contraseña: " + e.getMessage()));
        }
    }
}