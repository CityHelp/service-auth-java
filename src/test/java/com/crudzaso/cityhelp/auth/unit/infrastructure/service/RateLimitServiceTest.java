package com.crudzaso.cityhelp.auth.unit.infrastructure.service;

import com.crudzaso.cityhelp.auth.infrastructure.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService.
 * Tests rate limiting logic with mocked Redis operations.
 *
 * Test Coverage:
 * - Request allowed when under limit
 * - Request blocked when exceeding limit
 * - TTL set on first request
 * - IP extraction from request headers
 * - Identifier extraction from request
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService Unit Tests")
class RateLimitServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HttpServletRequest request;

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(redisTemplate);
    }

    @Test
    @DisplayName("Should allow request when under limit")
    void shouldAllowRequest_WhenUnderLimit() {
        // Arrange
        String keyPrefix = "login";
        String identifier = "user@example.com";
        int limit = 5;
        long windowSeconds = 300;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // Act
        boolean allowed = rateLimitService.isAllowed(keyPrefix, identifier, limit, windowSeconds);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations, times(1)).increment(anyString());
    }

    @Test
    @DisplayName("Should block request when exceeding limit")
    void shouldBlockRequest_WhenExceedingLimit() {
        // Arrange
        String keyPrefix = "login";
        String identifier = "user@example.com";
        int limit = 5;
        long windowSeconds = 300;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(6L);

        // Act
        boolean allowed = rateLimitService.isAllowed(keyPrefix, identifier, limit, windowSeconds);

        // Assert
        assertThat(allowed).isFalse();
        verify(valueOperations, times(1)).increment(anyString());
    }

    @Test
    @DisplayName("Should set TTL on first request")
    void shouldSetTTL_OnFirstRequest() {
        // Arrange
        String keyPrefix = "register";
        String identifier = "newuser@example.com";
        int limit = 3;
        long windowSeconds = 900;
        String expectedKey = "rate_limit:register:newuser@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedKey)).thenReturn(1L);
        when(redisTemplate.expire(expectedKey, windowSeconds, TimeUnit.SECONDS)).thenReturn(true);

        // Act
        boolean allowed = rateLimitService.isAllowed(keyPrefix, identifier, limit, windowSeconds);

        // Assert
        assertThat(allowed).isTrue();
        verify(valueOperations, times(1)).increment(expectedKey);
        verify(redisTemplate, times(1)).expire(expectedKey, windowSeconds, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void shouldExtractIp_FromXForwardedForHeader() {
        // Arrange
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.45, 192.168.1.1");

        // Act
        String identifier = rateLimitService.extractIdentifier(request);

        // Assert
        assertThat(identifier).isEqualTo("203.0.113.45");
        verify(request, times(1)).getHeader("X-Forwarded-For");
    }

    @Test
    @DisplayName("Should fallback to remote address when no proxy headers")
    void shouldExtractIp_FromRemoteAddress_WhenNoProxyHeaders() {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("192.168.100.50");

        // Act
        String identifier = rateLimitService.extractIdentifier(request);

        // Assert
        assertThat(identifier).isEqualTo("192.168.100.50");
        verify(request, times(1)).getRemoteAddr();
    }

    @Test
    @DisplayName("Should reset rate limit counter for specific key")
    void shouldResetRateLimit_ForSpecificKey() {
        // Arrange
        String keyPrefix = "login";
        String identifier = "user@example.com";
        String expectedKey = "rate_limit:login:user@example.com";

        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // Act
        rateLimitService.resetRateLimit(keyPrefix, identifier);

        // Assert
        verify(redisTemplate, times(1)).delete(expectedKey);
    }

    @Test
    @DisplayName("Should return current count for specific key")
    void shouldReturnCurrentCount_ForSpecificKey() {
        // Arrange
        String keyPrefix = "verify-email";
        String identifier = "user@example.com";
        String expectedKey = "rate_limit:verify-email:user@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(2);

        // Act
        long currentCount = rateLimitService.getCurrentCount(keyPrefix, identifier);

        // Assert
        assertThat(currentCount).isEqualTo(2L);
        verify(valueOperations, times(1)).get(expectedKey);
    }

    @Test
    @DisplayName("Should return zero when key does not exist")
    void shouldReturnZero_WhenKeyDoesNotExist() {
        // Arrange
        String keyPrefix = "login";
        String identifier = "newuser@example.com";
        String expectedKey = "rate_limit:login:newuser@example.com";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // Act
        long currentCount = rateLimitService.getCurrentCount(keyPrefix, identifier);

        // Assert
        assertThat(currentCount).isEqualTo(0L);
        verify(valueOperations, times(1)).get(expectedKey);
    }

    @Test
    @DisplayName("Should allow request when Redis returns null (fail open)")
    void shouldAllowRequest_WhenRedisReturnsNull() {
        // Arrange
        String keyPrefix = "login";
        String identifier = "user@example.com";
        int limit = 5;
        long windowSeconds = 300;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(null);

        // Act
        boolean allowed = rateLimitService.isAllowed(keyPrefix, identifier, limit, windowSeconds);

        // Assert
        assertThat(allowed).isTrue(); // Fail open: allow request on Redis failure
    }

    @Test
    @DisplayName("Should allow request when Redis throws exception (fail open)")
    void shouldAllowRequest_WhenRedisThrowsException() {
        // Arrange
        String keyPrefix = "login";
        String identifier = "user@example.com";
        int limit = 5;
        long windowSeconds = 300;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("Redis connection failed"));

        // Act
        boolean allowed = rateLimitService.isAllowed(keyPrefix, identifier, limit, windowSeconds);

        // Assert
        assertThat(allowed).isTrue(); // Fail open: allow request on Redis error
    }
}
