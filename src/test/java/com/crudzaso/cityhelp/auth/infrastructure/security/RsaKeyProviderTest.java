package com.crudzaso.cityhelp.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RsaKeyProviderTest {

    @Test
    void loadsPkcs8KeysWithDirectPem() {
        RsaKeyProvider provider = buildProvider(RsaKeyProviderTestKeys.PRIVATE_KEY, RsaKeyProviderTestKeys.PUBLIC_KEY);

        assertNotNull(provider.getPrivateKey());
        assertNotNull(provider.getPublicKey());
    }

    @Test
    void supportsPkcs1PrivateKey() {
        RsaKeyProvider provider = buildProvider(RsaKeyProviderTestKeys.PRIVATE_KEY_PKCS1, RsaKeyProviderTestKeys.PUBLIC_KEY);

        assertNotNull(provider.getPrivateKey());
        assertNotNull(provider.getPublicKey());
    }

    @Test
    void normalizesEscapedPemAndBase64() throws Exception {
        String escapedPrivate = RsaKeyProviderTestKeys.PRIVATE_KEY.replace("\n", "\\n");
        String escapedPublic = RsaKeyProviderTestKeys.PUBLIC_KEY.replace("\n", "\\n");

        RsaKeyProvider provider = buildProvider(escapedPrivate, escapedPublic);

        RSAPublicKey parsedPublic = provider.getPublicKey();
        assertEquals("RSA", parsedPublic.getAlgorithm());

        // also accept raw base64 strings
        String privateBase64 = stripHeaders(RsaKeyProviderTestKeys.PRIVATE_KEY);
        String publicBase64 = stripHeaders(RsaKeyProviderTestKeys.PUBLIC_KEY);
        RsaKeyProvider base64Provider = buildProvider(privateBase64, publicBase64);
        assertEquals(parsedPublic.getModulus(), base64Provider.getPublicKey().getModulus());
    }

    @Test
    void failsFastOnMismatchedKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair otherPair = generator.generateKeyPair();
        String wrongPublicPem = toPublicPem((RSAPublicKey) otherPair.getPublic());

        assertThrows(RuntimeException.class, () -> buildProvider(RsaKeyProviderTestKeys.PRIVATE_KEY, wrongPublicPem));
    }

    private RsaKeyProvider buildProvider(String privateKey, String publicKey) {
        RsaKeyProvider provider = new RsaKeyProvider();
        ReflectionTestUtils.setField(provider, "privateKeyPem", privateKey);
        ReflectionTestUtils.setField(provider, "publicKeyPem", publicKey);
        ReflectionTestUtils.setField(provider, "keyId", "test-key");
        provider.init();
        return provider;
    }

    private String stripHeaders(String pem) {
        return pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "");
    }

    private String toPublicPem(RSAPublicKey publicKey) {
        String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return """
                -----BEGIN PUBLIC KEY-----
                %s
                -----END PUBLIC KEY-----
                """.formatted(base64);
    }
}
