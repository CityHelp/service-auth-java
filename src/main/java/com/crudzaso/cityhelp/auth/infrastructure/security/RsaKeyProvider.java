package com.crudzaso.cityhelp.auth.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA Key Provider for JWT signing and verification.
 * Generates or loads RSA key pair (2048 bits) for RS256 algorithm.
 * Supports loading keys from configuration or auto-generation in development.
 */
@Component
public class RsaKeyProvider {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyProvider.class);
    private static final int KEY_SIZE = 2048;

    @Value("${app.jwt.rsa.private-key:}")
    private String privateKeyPem;

    @Value("${app.jwt.rsa.public-key:}")
    private String publicKeyPem;

    @Value("${app.jwt.rsa.key-id:cityhelp-key-1}")
    private String keyId;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;

    /**
     * Initialize RSA key pair after bean construction.
     * Loads from configuration or generates new keys if not provided.
     */
    @PostConstruct
    public void init() {
        try {
            if (privateKeyPem.isEmpty() || publicKeyPem.isEmpty()) {
                logger.warn("RSA keys not configured. Generating new key pair for development. " +
                        "DO NOT use this in production - configure app.jwt.rsa.private-key and app.jwt.rsa.public-key");
                generateKeyPair();
            } else {
                loadKeysFromConfiguration();
            }
            logger.info("RSA key provider initialized successfully with key ID: {}", keyId);
        } catch (Exception e) {
            logger.error("Failed to initialize RSA key provider", e);
            throw new RuntimeException("RSA key initialization failed", e);
        }
    }

    /**
     * Generate new RSA key pair (2048 bits).
     * Only used when keys are not provided in configuration.
     */
    private void generateKeyPair() throws NoSuchAlgorithmException {
        logger.info("Generating new RSA key pair (2048 bits)...");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        this.publicKey = (RSAPublicKey) keyPair.getPublic();

        // Log the generated keys in PEM format for configuration (development only)
        logger.info("Generated Private Key (PEM - SAVE THIS TO CONFIGURATION):\n{}",
                encodeKeyToPem(privateKey.getEncoded(), "PRIVATE KEY"));
        logger.info("Generated Public Key (PEM - SAVE THIS TO CONFIGURATION):\n{}",
                encodeKeyToPem(publicKey.getEncoded(), "PUBLIC KEY"));
    }

    /**
     * Load RSA keys from configuration (PEM format).
     */
    private void loadKeysFromConfiguration() throws Exception {
        logger.info("Loading RSA keys from configuration...");

        // Remove PEM headers/footers and whitespace
        String privateKeyContent = cleanPemKey(privateKeyPem);
        String publicKeyContent = cleanPemKey(publicKeyPem);

        // Decode Base64
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyContent);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyContent);

        // Generate key objects
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        this.publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

        logger.info("RSA keys loaded successfully from configuration");
    }

    /**
     * Clean PEM key format (remove headers, footers, whitespace).
     */
    private String cleanPemKey(String pemKey) {
        return pemKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    /**
     * Encode key bytes to PEM format string.
     */
    private String encodeKeyToPem(byte[] keyBytes, String keyType) {
        String base64Key = Base64.getEncoder().encodeToString(keyBytes);
        StringBuilder pem = new StringBuilder();
        pem.append("-----BEGIN ").append(keyType).append("-----\n");

        // Split into 64-character lines
        for (int i = 0; i < base64Key.length(); i += 64) {
            int endIndex = Math.min(i + 64, base64Key.length());
            pem.append(base64Key, i, endIndex).append("\n");
        }

        pem.append("-----END ").append(keyType).append("-----");
        return pem.toString();
    }

    /**
     * Get RSA private key for JWT signing.
     */
    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    /**
     * Get RSA public key for JWT verification.
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Get key ID for JWKS.
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Get public key modulus (n) for JWKS - Base64 URL encoded.
     */
    public String getPublicKeyModulus() {
        byte[] modulusBytes = publicKey.getModulus().toByteArray();
        // Remove leading zero byte if present (RSA modulus may have it)
        int startIndex = (modulusBytes[0] == 0) ? 1 : 0;
        byte[] cleanModulus = new byte[modulusBytes.length - startIndex];
        System.arraycopy(modulusBytes, startIndex, cleanModulus, 0, cleanModulus.length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(cleanModulus);
    }

    /**
     * Get public key exponent (e) for JWKS - Base64 URL encoded.
     */
    public String getPublicKeyExponent() {
        byte[] exponentBytes = publicKey.getPublicExponent().toByteArray();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(exponentBytes);
    }
}
