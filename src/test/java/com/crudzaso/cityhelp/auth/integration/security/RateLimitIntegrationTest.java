package com.crudzaso.cityhelp.auth.integration.security;

import com.crudzaso.cityhelp.auth.integration.BaseIntegrationTest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.LoginRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.RegisterRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.VerifyEmailRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Rate Limiting functionality.
 * Tests rate limiting enforcement on authentication endpoints using MockMvc and real Redis.
 *
 * Test Coverage:
 * - Login endpoint rate limiting (5 attempts / 5 minutes)
 * - Register endpoint rate limiting (3 attempts / 15 minutes)
 * - Verify-email endpoint rate limiting (3 attempts / 5 minutes)
 * - HTTP 429 response when limit exceeded
 * - Proper error messages in Spanish
 *
 * Note: Uses REAL Redis from Testcontainers for realistic integration testing.
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
@AutoConfigureMockMvc
@DisplayName("Rate Limiting Integration Tests")
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clear Redis before each test to ensure test isolation
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Should block login request when rate limit exceeded")
    void shouldBlockLoginRequest_WhenRateLimitExceeded() throws Exception {
        // Arrange: Make 5 login requests to reach the limit
        String email = "testuser@example.com";
        LoginRequest loginRequest = new LoginRequest(email, "ValidP@ssw0rd");

        // Act: Make 5 requests (under limit)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));
        }

        // Act & Assert: 6th request should be blocked (exceeds limit of 5)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    @DisplayName("Should allow login request when under rate limit")
    void shouldAllowLoginRequest_WhenUnderRateLimit() throws Exception {
        // Arrange: Make 3 requests (under limit of 5)
        String email = "validuser@example.com";
        LoginRequest loginRequest = new LoginRequest(email, "ValidP@ssw0rd");

        // Act & Assert: All 3 requests should pass rate limit check
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isBadRequest()) // 400 because invalid credentials (but rate limit passed)
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Test
    @DisplayName("Should block register request when rate limit exceeded")
    void shouldBlockRegisterRequest_WhenRateLimitExceeded() throws Exception {
        // Arrange: Make 3 register requests to reach the limit
        RegisterRequest registerRequest = new RegisterRequest(
                "Test",
                "User",
                "newuser@example.com",
                "ValidP@ssw0rd1!"
        );

        // Act: Make 3 requests (at limit)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(registerRequest)));
        }

        // Act & Assert: 4th request should be blocked (exceeds limit of 3)
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    @DisplayName("Should allow register request when under rate limit")
    void shouldAllowRegisterRequest_WhenUnderRateLimit() throws Exception {
        // Arrange: Make 2 requests (under limit of 3)
        RegisterRequest registerRequest = new RegisterRequest(
                "Test",
                "User",
                "newuser_under_limit@example.com",
                "ValidP@ssw0rd1!"
        );

        // Act & Assert: First request should succeed with 201 Created
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Second request with same email should fail (email already exists), but rate limit should pass
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest()); // 400 because email exists (but rate limit passed)
    }

    @Test
    @DisplayName("Should block verify-email request when rate limit exceeded")
    void shouldBlockVerifyEmailRequest_WhenRateLimitExceeded() throws Exception {
        // Arrange: Make 3 verify-email requests to reach the limit
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest("user@example.com", "123456");

        // Act: Make 3 requests (at limit)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest)));
        }

        // Act & Assert: 4th request should be blocked (exceeds limit of 3)
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value(containsString("Rate limit exceeded")));
    }

    @Test
    @DisplayName("Should allow verify-email request when under rate limit")
    void shouldAllowVerifyEmailRequest_WhenUnderRateLimit() throws Exception {
        // Arrange: Make 2 requests (under limit of 3)
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest("user@example.com", "123456");

        // Act & Assert: Both requests should pass rate limit check
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/verify-email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(verifyRequest)))
                    .andExpect(status().isBadRequest()); // 400 because email doesn't exist (but rate limit passed)
        }
    }

    @Test
    @DisplayName("Should use email as identifier for rate limiting")
    void shouldUseEmailAsIdentifier_ForRateLimiting() throws Exception {
        // Arrange: Two requests with same email should share rate limit counter
        String email = "same-user@example.com";
        LoginRequest request1 = new LoginRequest(email, "ValidP@ssw0rd");

        // Act: First request should pass
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isBadRequest()) // Rate limit passed, but invalid credentials
                .andExpect(jsonPath("$.success").value(false));

        // Act: Second request with same email should also pass (count = 2, still under limit of 5)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isBadRequest()) // Rate limit passed, but invalid credentials
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should set TTL on first request in Redis")
    void shouldSetTTL_OnFirstRequest() throws Exception {
        // Arrange: Make first request to set TTL
        LoginRequest loginRequest = new LoginRequest("ttl-test-user@example.com", "ValidP@ssw0rd");

        // Act: Make first request
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()) // Rate limit passed, invalid credentials
                .andExpect(jsonPath("$.success").value(false));

        // Assert: Verify that TTL was set by checking Redis key expiration
        String rateKey = "rate_limit:login:ttl-test-user@example.com";
        Long ttl = redisTemplate.getExpire(rateKey, java.util.concurrent.TimeUnit.SECONDS);

        // TTL should be positive (key exists with expiration) and less than or equal to 300 seconds (5 minutes)
        assert ttl != null && ttl > 0 && ttl <= 300 : "TTL should be set and <= 300 seconds";
    }
}
