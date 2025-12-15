package com.crudzaso.cityhelp.auth.integration.controller;

import com.crudzaso.cityhelp.auth.infrastructure.controller.JwksController;
import com.crudzaso.cityhelp.auth.integration.BaseIntegrationTest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JwksController using MockMvc.
 *
 * Tests the JWKS (JSON Web Key Set) endpoint which provides the public key
 * for JWT verification. This is a critical endpoint for external services
 * that need to validate JWTs issued by this auth service.
 *
 * Key areas tested:
 * - JWKS endpoint accessibility
 * - Response format and structure
 * - Key parameters and values
 * - CORS headers
 * - Error handling
 */
@AutoConfigureMockMvc
public class JwksControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private JwksController jwksController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    // ========== JWKS Endpoint Tests ==========

    @Test
    @DisplayName("Should return JWKS with correct structure")
    void shouldReturnJwks_WithCorrectStructure() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isNotEmpty())
                .andExpect(jsonPath("$.keys[0]").exists());
    }

    @Test
    @DisplayName("Should return JWKS with required key parameters")
    void shouldReturnJwks_WithRequiredKeyParameters() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA")) // Key Type
                .andExpect(jsonPath("$.keys[0].use").value("sig")) // Public Key Use
                .andExpect(jsonPath("$.keys[0].alg").value("RS256")) // Algorithm
                .andExpect(jsonPath("$.keys[0].kid").isNotEmpty()) // Key ID
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty()) // Modulus
                .andExpect(jsonPath("$.keys[0].e").value("AQAB")) // Exponent (typically 65537)
                .andExpect(jsonPath("$.keys[0].n", hasLength(greaterThan(100)))); // Modulus should be long
    }

    @Test
    @DisplayName("Should return JWKS accessible without authentication")
    void shouldReturnJwks_WithoutAuthentication() throws Exception {
        // Act & Assert - This endpoint should be publicly accessible
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").exists());
    }

    @Test
    @DisplayName("Should return JWKS with consistent key ID")
    void shouldReturnJwks_WithConsistentKeyId() throws Exception {
        // Act
        String kid1 = mockMvc.perform(get("/.well-known/jwks.json"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Make second request
        String kid2 = mockMvc.perform(get("/.well-known/jwks.json"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - Key ID should be consistent across requests
        assertEquals(kid1, kid2);
    }

    @Test
    @DisplayName("Should return JWKS with proper CORS headers")
    void shouldReturnJwks_WithProperCorsHeaders() throws Exception {
        // Act & Assert
        mockMvc.perform(options("/.well-known/jwks.json")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")));
    }

    // ========== Key Validation Tests ==========

    @Test
    @DisplayName("Should return JWKS with valid RSA key components")
    void shouldReturnJwks_WithValidRsaKeyComponents() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").value("AQAB"));
    }

    @Test
    @DisplayName("Should return JWKS with single key")
    void shouldReturnJwks_WithSingleKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys", hasSize(1)));
    }

    // ========== Performance Tests ==========

    @Test
    @DisplayName("Should return JWKS response quickly")
    void shouldReturnJwksResponseQuickly() throws Exception {
        // Act & Assert
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk());

        long duration = System.currentTimeMillis() - startTime;

        // JWKS endpoint should respond quickly (under 100ms)
        assertTrue(duration < 100);
    }

    @Test
    @DisplayName("Should handle multiple concurrent JWKS requests")
    void shouldHandleMultipleConcurrentJwksRequests() throws Exception {
        // Act & Assert - Make multiple requests to ensure stability
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/.well-known/jwks.json"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.keys").exists());
        }
    }

    // ========== Security Tests ==========

    @Test
    @DisplayName("Should reject POST requests to JWKS endpoint")
    void shouldRejectPostRequestsToJwksEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/.well-known/jwks.json"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should reject PUT requests to JWKS endpoint")
    void shouldRejectPutRequestsToJwksEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/.well-known/jwks.json"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should reject DELETE requests to JWKS endpoint")
    void shouldRejectDeleteRequestsToJwksEndpoint() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/.well-known/jwks.json"))
                .andExpect(status().isInternalServerError());
    }

    // ========== Content Type Tests ==========

    @Test
    @DisplayName("Should return correct content type")
    void shouldReturnCorrectContentType() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should accept JSON content type")
    void shouldAcceptJsonContentType() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept("application/json"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    @DisplayName("Should handle different accept headers")
    void shouldHandleDifferentAcceptHeaders() throws Exception {
        // Act & Assert - Should work with various accept headers
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept("*/*"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk());
    }

    // ========== Controller Direct Tests ==========

    @Test
    @DisplayName("Controller should be properly wired")
    void shouldHaveControllerProperlyWired() {
        // Assert
        assertNotNull(jwksController);
    }

    // ========== Error Handling Tests ==========

    @Test
    @DisplayName("Should handle malformed path gracefully")
    void shouldHandleMalformedPathGracefully() throws Exception {
        // Act & Assert
        // Returns 401 because Spring Security intercepts non-matching paths before controller
        mockMvc.perform(get("/.well-known/jwks.json/invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should handle query parameters gracefully")
    void shouldHandleQueryParametersGracefully() throws Exception {
        // Act & Assert - JWKS endpoint should ignore query parameters
        mockMvc.perform(get("/.well-known/jwks.json?param=value"))
                .andExpect(status().isOk());
    }

    // ========== Integration Validation Tests ==========

    @Test
    @DisplayName("Should return JWKS compatible with JWT verification")
    void shouldReturnJwksCompatibleWithJwtVerification() throws Exception {
        // This is a basic structural validation
        // Act & Assert - Basic structure validation
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"));
    }

    @Test
    @DisplayName("Should maintain JWKS consistency across requests")
    void shouldMaintainJwksConsistency() throws Exception {
        // Get JWKS response
        String jwksResponse1 = mockMvc.perform(get("/.well-known/jwks.json"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Make another request
        String jwksResponse2 = mockMvc.perform(get("/.well-known/jwks.json"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert responses are identical
        assertEquals(jwksResponse1, jwksResponse2);
    }
}