package com.crudzaso.cityhelp.auth.infrastructure.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enable rate limiting on a method.
 * Uses Redis to track request counts per key within a time window.
 *
 * Usage example:
 * <pre>
 * {@code @RateLimited(key = "login", limit = 5, windowSeconds = 300)}
 * public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
 *     // method implementation
 * }
 * </pre>
 *
 * This annotation triggers AOP interception via {@link RateLimitAspect} which:
 * 1. Extracts an identifier (email from request body or IP address)
 * 2. Checks if the request count exceeds the limit within the time window
 * 3. Throws {@link com.crudzaso.cityhelp.auth.application.exception.TooManyRequestsException} if limit exceeded
 * 4. Allows the request to proceed if within limits
 *
 * Redis Key Format: rate_limit:{key}:{identifier}
 * Example: rate_limit:login:user@example.com or rate_limit:login:192.168.1.100
 *
 * @author CityHelp Team
 * @since 1.0.0
 * @see RateLimitAspect
 * @see com.crudzaso.cityhelp.auth.infrastructure.service.RateLimitService
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Rate limit key prefix that identifies the operation type.
     * This prefix is combined with a user identifier (email or IP) to create unique Redis keys.
     *
     * Examples: "login", "register", "verify-email", "reset-password"
     *
     * @return the rate limit key prefix
     */
    String key();

    /**
     * Maximum number of requests allowed within the time window.
     * After reaching this limit, subsequent requests will be rejected with HTTP 429.
     *
     * Examples:
     * - Login: 5 attempts (anti-brute force)
     * - Register: 3 attempts (prevent spam)
     * - Verify Email: 3 attempts (prevent code guessing)
     *
     * @return the maximum request limit
     */
    int limit();

    /**
     * Time window in seconds during which the limit applies.
     * After this time expires, the counter resets automatically (Redis TTL).
     *
     * Examples:
     * - 300 seconds = 5 minutes
     * - 900 seconds = 15 minutes
     * - 3600 seconds = 1 hour
     *
     * @return the time window duration in seconds
     */
    long windowSeconds();
}
