package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.infrastructure.security.RsaKeyProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * JWKS (JSON Web Key Set) endpoint controller for external service JWT verification.
 * Exposes RSA public keys in JWKS format (RFC 7517) for external services (C#, Python, etc.) to verify JWTs.
 * Critical component for multi-platform ecosystem integration.
 *
 * Standards:
 * - RFC 7517: JSON Web Key (JWK)
 * - RFC 7518: JSON Web Algorithms (JWA)
 * - RFC 7519: JSON Web Token (JWT)
 */
@RestController
@RequestMapping("/.well-known")
@Tag(name = "JWKS - Public Key Discovery", description = "JSON Web Key Set endpoint for external service JWT verification. " +
        "Used by external services (C#, Python, Node.js, etc.) to retrieve public keys for verifying JWTs issued by the Auth Service. " +
        "Implements RFC 7517 (JSON Web Key) standard.")
public class JwksController {

    private static final Logger logger = LoggerFactory.getLogger(JwksController.class);
    private final RsaKeyProvider rsaKeyProvider;

    public JwksController(RsaKeyProvider rsaKeyProvider) {
        this.rsaKeyProvider = rsaKeyProvider;
    }

    /**
     * Get JWKS (JSON Web Key Set) endpoint.
     * Returns public keys in JWKS format for external services to verify JWTs.
     *
     * JWKS Format (RFC 7517):
     * {
     *   "keys": [
     *     {
     *       "kty": "RSA",               // Key Type
     *       "use": "sig",               // Public Key Use (signature)
     *       "alg": "RS256",             // Algorithm
     *       "kid": "cityhelp-key-1",   // Key ID (matches JWT header)
     *       "n": "...",                 // RSA modulus (Base64 URL encoded)
     *       "e": "AQAB"                 // RSA exponent (Base64 URL encoded)
     *     }
     *   ]
     * }
     *
     * @return JWKS response with RSA public key
     */
    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get JSON Web Key Set (JWKS)",
            description = "Retrieves the JSON Web Key Set containing RSA public keys used to verify JWTs issued by the Auth Service. " +
                    "This endpoint is used by external services (C#, Python, Node.js, etc.) to validate JWT signatures. " +
                    "The response follows RFC 7517 JSON Web Key format with Base64 URL-encoded RSA components. " +
                    "\n\nUsage Example (C# service): Use HttpClient to fetch this endpoint, extract public key 'n' and 'e', " +
                    "and use them to verify JWT signatures using JOSE/JWT libraries."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "JWKS response with public keys",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    description = "JSON Web Key Set containing RSA public keys",
                                    example = "{\"keys\":[{\"kty\":\"RSA\",\"use\":\"sig\",\"alg\":\"RS256\"," +
                                            "\"kid\":\"cityhelp-key-1\",\"n\":\"xGOr-H7A-PWnP5...\",\"e\":\"AQAB\"}]}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error - Unable to generate JWKS response"
            )
    })
    public ResponseEntity<Map<String, Object>> getJwks() {
        logger.info("JWKS endpoint called - providing public key for external service verification");

        try {
            // Build JWKS response according to RFC 7517
            Map<String, Object> jwk = Map.of(
                    "kty", "RSA",                                      // Key Type: RSA
                    "use", "sig",                                      // Public Key Use: signature
                    "alg", "RS256",                                    // Algorithm: RS256
                    "kid", rsaKeyProvider.getKeyId(),                  // Key ID (matches JWT header)
                    "n", rsaKeyProvider.getPublicKeyModulus(),         // RSA modulus (Base64 URL encoded)
                    "e", rsaKeyProvider.getPublicKeyExponent()         // RSA exponent (Base64 URL encoded)
            );

            Map<String, Object> jwksResponse = Map.of("keys", List.of(jwk));

            logger.debug("JWKS response generated successfully with key ID: {}", rsaKeyProvider.getKeyId());
            return ResponseEntity.ok(jwksResponse);

        } catch (Exception e) {
            logger.error("Error generating JWKS response", e);
            // Return error but don't expose sensitive details
            return ResponseEntity.internalServerError().build();
        }
    }
}