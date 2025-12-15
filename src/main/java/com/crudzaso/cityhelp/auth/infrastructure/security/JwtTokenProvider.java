package com.crudzaso.cityhelp.auth.infrastructure.security;

import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

/**
 * JWT token provider for CityHelp Auth Service.
 * Handles JWT token generation, validation, and parsing.
 * Uses RS256 algorithm with RSA key pair for JWKS endpoint support.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final RsaKeyProvider rsaKeyProvider;

    @Value("${app.jwt.expiration-in-ms:86400000}") // 24 hours
    private long jwtExpirationInMs;

    @Value("${app.jwt.refresh-expiration-in-ms:604800000}") // 7 days
    private long refreshExpirationInMs;

    public JwtTokenProvider(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    private RSAPrivateKey getSigningKey() {
        return rsaKeyProvider.getPrivateKey();
    }

    private RSAPublicKey getVerificationKey() {
        return rsaKeyProvider.getPublicKey();
    }

    private void ensureKeysPresent() {
        if (rsaKeyProvider.getPrivateKey() == null || rsaKeyProvider.getPublicKey() == null) {
            throw new IllegalStateException("RSA keys are not initialized. Check RsaKeyProvider configuration.");
        }
    }

    /**
     * Generate JWT access token for authenticated user.
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        ensureKeysPresent();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .header()
                    .keyId(rsaKeyProvider.getKeyId())
                    .and()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "access")
                .signWith(getSigningKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate JWT access token directly with user details (without Authentication object).
     *
     * @param userId User ID
     * @param email User email (used as subject)
     * @param role User role
     * @return JWT access token string
     */
    public String generateToken(Long userId, String email, String role) {
        ensureKeysPresent();
        Objects.requireNonNull(userId, "userId is required to build JWT");
        Objects.requireNonNull(email, "email/subject is required to build JWT");
        Objects.requireNonNull(role, "role is required to build JWT");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .header()
                    .keyId(rsaKeyProvider.getKeyId())
                    .and()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("user_id", userId)
                .claim("userId", userId) // legacy compatibility
                .claim("role", role)
                .claim("type", "access")
                .signWith(getSigningKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Generate JWT refresh token for authenticated user.
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        ensureKeysPresent();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

        return Jwts.builder()
                .header()
                    .keyId(rsaKeyProvider.getKeyId())
                    .and()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "refresh")
                .signWith(getSigningKey(), Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Get username from JWT token.
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Validate JWT token.
     */
    public boolean validateToken(String token) {
        try {
            ensureKeysPresent();
            Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Get expiration date from JWT token.
     */
    public Date getExpirationFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getExpiration();
    }

    /**
     * Get token type from JWT token (access/refresh).
     */
    public String getTokenTypeFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("type", String.class);
    }

    /**
     * Get remaining time in seconds until token expires.
     */
    public long getRemainingTimeInSeconds(String token) {
        Date expiration = getExpirationFromJWT(token);
        Date now = new Date();
        return (expiration.getTime() - now.getTime()) / 1000;
    }

    /**
     * Get user ID from JWT token.
     *
     * @param token JWT token
     * @return User ID extracted from the token
     */
    public Long getUserIdFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getVerificationKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }
}