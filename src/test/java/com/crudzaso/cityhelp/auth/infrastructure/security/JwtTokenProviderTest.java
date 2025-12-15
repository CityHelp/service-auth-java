package com.crudzaso.cityhelp.auth.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private RsaKeyProvider rsaKeyProvider;

    @BeforeEach
    void setUp() {
        rsaKeyProvider = new RsaKeyProvider();
        ReflectionTestUtils.setField(rsaKeyProvider, "privateKeyPem", RsaKeyProviderTestKeys.PRIVATE_KEY);
        ReflectionTestUtils.setField(rsaKeyProvider, "publicKeyPem", RsaKeyProviderTestKeys.PUBLIC_KEY);
        ReflectionTestUtils.setField(rsaKeyProvider, "keyId", "test-key");
        rsaKeyProvider.init();

        tokenProvider = new JwtTokenProvider(rsaKeyProvider);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 3600_000L);
        ReflectionTestUtils.setField(tokenProvider, "refreshExpirationInMs", 7200_000L);
    }

    @Test
    void generatesTokenWithExpectedClaims() {
        String token = tokenProvider.generateToken(99L, "user@example.com", "ADMIN");

        Claims claims = Jwts.parser()
                .verifyWith(rsaKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("user@example.com", claims.getSubject());
        assertEquals(99L, claims.get("user_id", Long.class));
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void validateTokenFailsWithDifferentKey() {
        String token = tokenProvider.generateToken(10L, "other@example.com", "USER");

        RsaKeyProvider wrongProvider = new RsaKeyProvider();
        ReflectionTestUtils.setField(wrongProvider, "privateKeyPem", RsaKeyProviderTestKeys.PRIVATE_KEY_PKCS1);
        ReflectionTestUtils.setField(wrongProvider, "publicKeyPem", RsaKeyProviderTestKeys.PUBLIC_KEY);
        ReflectionTestUtils.setField(wrongProvider, "keyId", "another-key");
        wrongProvider.init();

        JwtTokenProvider wrongTokenProvider = new JwtTokenProvider(wrongProvider);

        assertFalse(wrongTokenProvider.validateToken(token));
    }

    @Test
    void generatesAccessTokenFromAuthentication() {
        User user = new User("demo@example.com", "password", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        String token = tokenProvider.generateAccessToken(authentication);

        assertTrue(tokenProvider.validateToken(token));
    }
}
