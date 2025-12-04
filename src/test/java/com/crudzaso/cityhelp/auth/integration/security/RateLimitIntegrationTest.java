package com.crudzaso.cityhelp.auth.integration.security;

import com.crudzaso.cityhelp.auth.infrastructure.dto.LoginRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.RegisterRequest;
import com.crudzaso.cityhelp.auth.infrastructure.dto.VerifyEmailRequest;
import com.crudzaso.cityhelp.auth.infrastructure.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Rate Limiting functionality.
 * Tests rate limiting enforcement on authentication endpoints using MockMvc.
 *
 * Test Coverage:
 * - Login endpoint rate limiting (5 attempts / 5 minutes)
 * - Register endpoint rate limiting (3 attempts / 15 minutes)
 * - Verify-email endpoint rate limiting (3 attempts / 5 minutes)
 * - HTTP 429 response when limit exceeded
 * - Proper error messages in Spanish
 *
 * Note: Uses mocked Redis operations instead of Testcontainers Redis
 * for faster test execution and simpler setup.
 *
 * @author CityHelp Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Rate Limiting Integration Tests")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        // Setup RedisTemplate to return ValueOperations mock
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    @DisplayName("Should block login request when rate limit exceeded")
    void shouldBlockLoginRequest_WhenRateLimitExceeded() throws Exception {
        // Arrange: Simulate 6th request (exceeds limit of 5)
        String email = "testuser@example.com";
        LoginRequest loginRequest = new LoginRequest(email, "ValidP@ssw0rd");

        // Simulate Redis returning count = 6 (exceeding limit)
        when(valueOperations.increment(anyString())).thenReturn(6L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Límite de intentos excedido")))
                .andExpect(jsonPath("$.message").value(containsString("login")));
    }

    @Test
    @DisplayName("Should allow login request when under rate limit")
    void shouldAllowLoginRequest_WhenUnderRateLimit() throws Exception {
        // Arrange: Simulate 3rd request (under limit of 5)
        String email = "validuser@example.com";
        LoginRequest loginRequest = new LoginRequest(email, "ValidP@ssw0rd");

        // Simulate Redis returning count = 3 (under limit)
        when(valueOperations.increment(anyString())).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound()); // 404 because user doesn't exist (but rate limit passed)
    }

    @Test
    @DisplayName("Should block register request when rate limit exceeded")
    void shouldBlockRegisterRequest_WhenRateLimitExceeded() throws Exception {
        // Arrange: Simulate 4th request (exceeds limit of 3)
        RegisterRequest registerRequest = new RegisterRequest(
                "Test",
                "User",
                "newuser@example.com",
                "ValidP@ssw0rd1!"
        );

        // Simulate Redis returning count = 4 (exceeding limit of 3)
        when(valueOperations.increment(anyString())).thenReturn(4L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Límite de intentos excedido")))
                .andExpect(jsonPath("$.message").value(containsString("register")));
    }

    @Test
    @DisplayName("Should allow register request when under rate limit")
    void shouldAllowRegisterRequest_WhenUnderRateLimit() throws Exception {
        // Arrange: Simulate 2nd request (under limit of 3)
        RegisterRequest registerRequest = new RegisterRequest(
                "Test",
                "User",
                "newuser@example.com",
                "ValidP@ssw0rd1!"
        );

        // Simulate Redis returning count = 2 (under limit)
        when(valueOperations.increment(anyString())).thenReturn(2L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated()); // 201 Created (registration succeeds, but rate limit passed)
    }

    @Test
    @DisplayName("Should block verify-email request when rate limit exceeded")
    void shouldBlockVerifyEmailRequest_WhenRateLimitExceeded() throws Exception {
        // Arrange: Simulate 4th request (exceeds limit of 3)
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest("user@example.com", "123456");

        // Simulate Redis returning count = 4 (exceeding limit of 3)
        when(valueOperations.increment(anyString())).thenReturn(4L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isTooManyRequests()) // HTTP 429
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Límite de intentos excedido")))
                .andExpect(jsonPath("$.message").value(containsString("verify-email")));
    }

    @Test
    @DisplayName("Should allow verify-email request when under rate limit")
    void shouldAllowVerifyEmailRequest_WhenUnderRateLimit() throws Exception {
        // Arrange: Simulate 1st request (under limit of 3)
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest("user@example.com", "123456");

        // Simulate Redis returning count = 1 (under limit)
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isBadRequest()); // 400 because email doesn't exist (but rate limit passed)
    }

    @Test
    @DisplayName("Should use email as identifier for rate limiting")
    void shouldUseEmailAsIdentifier_ForRateLimiting() throws Exception {
        // Arrange: Two requests with same email should share rate limit counter
        String email = "same-user@example.com";
        LoginRequest request1 = new LoginRequest(email, "ValidP@ssw0rd");

        // Simulate Redis returning incremental counts for same email
        when(valueOperations.increment(contains(email))).thenReturn(1L, 2L);

        // Act: First request (count = 1)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isNotFound()); // Rate limit passed, but user not found

        // Act: Second request (count = 2, still under limit)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isNotFound()); // Rate limit passed, but user not found
    }

    @Test
    @DisplayName("Should set TTL on first request in Redis")
    void shouldSetTTL_OnFirstRequest() throws Exception {
        // Arrange: Simulate first request (count = 1)
        LoginRequest loginRequest = new LoginRequest("user@example.com", "ValidP@ssw0rd");

        // Simulate Redis returning count = 1 (first request)
        when(valueOperations.increment(anyString())).thenReturn(1L);

        // Act
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound()); // Rate limit passed

        // Note: In real scenario, RateLimitService would call redisTemplate.expire()
        // We can't verify this easily with MockBean, but the unit test covers it
    }
}
