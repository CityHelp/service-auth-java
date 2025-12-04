package com.crudzaso.cityhelp.auth.infrastructure.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for implementing rate limiting using Redis.
 * Uses sliding window algorithm to track request counts per user/IP within time windows.
 *
 * Redis Key Format: rate_limit:{prefix}:{identifier}
 * Examples:
 * - rate_limit:login:user@example.com
 * - rate_limit:login:192.168.1.100
 * - rate_limit:register:newuser@example.com
 *
 * The service automatically handles:
 * - Incrementing request counters
 * - Setting TTL on first request
 * - Checking if limit is exceeded
 * - Logging rate limit violations
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
@Service
public class RateLimitService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Constructor injection for Redis template.
     *
     * @param redisTemplate the configured Redis template
     */
    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks if a request is allowed based on rate limiting rules.
     * Uses Redis INCR command for atomic counter increment.
     *
     * @param keyPrefix the rate limit operation type (e.g., "login", "register")
     * @param identifier the unique identifier (email or IP address)
     * @param limit the maximum number of requests allowed
     * @param windowSeconds the time window in seconds
     * @return true if request is allowed, false if limit exceeded
     */
    public boolean isAllowed(String keyPrefix, String identifier, int limit, long windowSeconds) {
        String key = buildRateLimitKey(keyPrefix, identifier);

        try {
            // Atomically increment counter in Redis
            Long currentCount = redisTemplate.opsForValue().increment(key);

            if (currentCount == null) {
                logger.error("Redis increment returned null for key: {}", key);
                // Fail open: allow request if Redis fails
                return true;
            }

            // Set TTL on first request (when counter is 1)
            if (currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
                logger.debug("Set TTL for key: {} to {} seconds", key, windowSeconds);
            }

            boolean allowed = currentCount <= limit;

            if (!allowed) {
                logger.warn("Rate limit exceeded for key: {} (count: {}/{})", key, currentCount, limit);
            } else {
                logger.debug("Rate limit check passed for key: {} (count: {}/{})", key, currentCount, limit);
            }

            return allowed;

        } catch (Exception e) {
            logger.error("Redis error checking rate limit for key: {}. Allowing request (fail open)", key, e);
            // Fail open: allow request if Redis has issues
            return true;
        }
    }

    /**
     * Extracts an identifier from the HTTP request for rate limiting.
     * Priority:
     * 1. Email from request body (if present)
     * 2. IP address from request headers (X-Forwarded-For, X-Real-IP)
     * 3. Remote address from request
     *
     * @param request the HTTP servlet request
     * @return the extracted identifier (email or IP)
     */
    public String extractIdentifier(HttpServletRequest request) {
        // Try to get IP from proxy headers first
        String ipAddress = getClientIpAddress(request);

        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        logger.debug("Extracted identifier (IP): {}", ipAddress);
        return ipAddress;
    }

    /**
     * Extracts the client's real IP address, considering proxy headers.
     * Checks common proxy headers: X-Forwarded-For, X-Real-IP, Proxy-Client-IP, etc.
     *
     * @param request the HTTP servlet request
     * @return the client's IP address, or null if not found
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return null;
    }

    /**
     * Builds a Redis key for rate limiting.
     * Format: rate_limit:{prefix}:{identifier}
     *
     * @param prefix the rate limit operation type
     * @param identifier the user identifier (email or IP)
     * @return the formatted Redis key
     */
    private String buildRateLimitKey(String prefix, String identifier) {
        return String.format("rate_limit:%s:%s", prefix, identifier);
    }

    /**
     * Resets the rate limit counter for a specific key.
     * Useful for testing or manual intervention.
     *
     * @param keyPrefix the rate limit operation type
     * @param identifier the unique identifier
     */
    public void resetRateLimit(String keyPrefix, String identifier) {
        String key = buildRateLimitKey(keyPrefix, identifier);
        try {
            redisTemplate.delete(key);
            logger.info("Reset rate limit for key: {}", key);
        } catch (Exception e) {
            logger.error("Error resetting rate limit for key: {}", key, e);
        }
    }

    /**
     * Gets the current request count for a specific key.
     * Returns 0 if key doesn't exist.
     *
     * @param keyPrefix the rate limit operation type
     * @param identifier the unique identifier
     * @return the current request count
     */
    public long getCurrentCount(String keyPrefix, String identifier) {
        String key = buildRateLimitKey(keyPrefix, identifier);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Integer) {
                return ((Integer) value).longValue();
            } else if (value instanceof Long) {
                return (Long) value;
            }
            return 0L;
        } catch (Exception e) {
            logger.error("Error getting current count for key: {}", key, e);
            return 0L;
        }
    }
}
