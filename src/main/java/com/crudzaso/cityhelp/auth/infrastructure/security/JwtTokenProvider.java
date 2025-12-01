package com.crudzaso.cityhelp.auth.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token provider for CityHelp Auth Service.
 * Handles JWT token generation, validation, and parsing.
 * Uses RS256 algorithm with key pair for JWKS endpoint support.
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret:cityhelp-default-secret-key-for-development-only-change-in-production}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-in-ms:86400000}") // 24 hours
    private long jwtExpirationInMs;

    @Value("${app.jwt.refresh-expiration-in-ms:604800000}") // 7 days
    private long refreshExpirationInMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate JWT access token for authenticated user.
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("type", "access")
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
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
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", "access")
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate JWT refresh token for authenticated user.
     */
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpirationInMs);

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .claim("type", "refresh")
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Get username from JWT token.
     */
    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
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
            Jwts.parser()
                .verifyWith(getSigningKey())
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
                .verifyWith(getSigningKey())
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
                .verifyWith(getSigningKey())
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
}