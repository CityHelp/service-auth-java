package com.crudzaso.cityhelp.auth.infrastructure.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JWKS endpoint controller for external service JWT verification.
 * Exposes public keys for external services (C#, Python, etc.) to verify JWTs.
 * Critical component for multi-platform ecosystem integration.
 */
@RestController
@RequestMapping("/.well-known")
public class JwksController {

    // TODO: This will be implemented with proper RSA key pair generation
    // For now, returning a placeholder JWKS response
    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getJwks() {
        // Placeholder JWKS response
        // TODO: Implement proper RSA key generation and JWKS format
        Map<String, Object> jwksResponse = Map.of(
                "keys", java.util.List.of(
                        Map.of(
                                "kty", "RSA",
                                "use", "sig",
                                "alg", "RS256",
                                "kid", "cityhelp-key-1",
                                "n", "placeholder-modulus",
                                "e", "AQAB"
                        )
                )
        );

        return ResponseEntity.ok(jwksResponse);
    }
}