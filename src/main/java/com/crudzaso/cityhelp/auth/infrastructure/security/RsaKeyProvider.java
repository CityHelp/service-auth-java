package com.crudzaso.cityhelp.auth.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
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
            boolean privateConfigured = privateKeyPem != null && !privateKeyPem.isBlank();
            boolean publicConfigured = publicKeyPem != null && !publicKeyPem.isBlank();

            if (privateConfigured != publicConfigured) {
                throw new IllegalStateException("RSA key pair configuration is incomplete. Provide both private and public keys or none.");
            }

            if (!privateConfigured) {
                logger.warn("RSA keys not configured. Generating new key pair for development. " +
                        "DO NOT use this in production - configure app.jwt.rsa.private-key and app.jwt.rsa.public-key");
                generateKeyPair();
            } else {
                loadKeysFromConfiguration();
            }

            validateKeyPair();
            selfTestJwtSigning();

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

        String normalizedPrivateKey = normalizeKey(privateKeyPem);
        String normalizedPublicKey = normalizeKey(publicKeyPem);

        if (normalizedPrivateKey.isBlank() || normalizedPublicKey.isBlank()) {
            throw new IllegalArgumentException("RSA key configuration is blank after normalization");
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        byte[] privateKeyBytes = decodePrivateKey(normalizedPrivateKey);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        this.privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

        byte[] publicKeyBytes = decodePublicKey(normalizedPublicKey);
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
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key
                .replace("\\n", "\n")
                .replace("\r", "")
                .trim();
    }

    private byte[] decodePrivateKey(String pemFormattedKey) {
        try {
            String cleaned = cleanPemKey(pemFormattedKey);
            byte[] keyBytes = Base64.getMimeDecoder().decode(cleaned);

            try {
                KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
                return keyBytes;
            } catch (InvalidKeySpecException pkcs8Ex) {
                byte[] converted = convertPkcs1ToPkcs8(keyBytes);
                try {
                    KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(converted));
                    return converted;
                } catch (InvalidKeySpecException inner) {
                    pkcs8Ex.addSuppressed(inner);
                    throw pkcs8Ex;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RSA private key format", e);
        }
    }

    private byte[] decodePublicKey(String pemFormattedKey) {
        try {
            String cleaned = cleanPemKey(pemFormattedKey);
            return Base64.getMimeDecoder().decode(cleaned);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid RSA public key format", e);
        }
    }

    private byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        // Wrap PKCS#1 bytes into PKCS#8 structure (RFC5208)
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;

        byte[] pkcs8Header = new byte[] {
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff),
                0x2, 0x1, 0x0,
                0x30, 0x0d,
                0x6, 0x9,
                0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x1, 0x1, 0x1,
                0x5, 0x0,
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff)
        };

        byte[] pkcs8Bytes = new byte[pkcs8Header.length + pkcs1Length];
        System.arraycopy(pkcs8Header, 0, pkcs8Bytes, 0, pkcs8Header.length);
        System.arraycopy(pkcs1Bytes, 0, pkcs8Bytes, pkcs8Header.length, pkcs1Length);
        return pkcs8Bytes;
    }

    private void validateKeyPair() throws Exception {
        if (privateKey == null || publicKey == null) {
            throw new IllegalStateException("RSA key pair not initialized");
        }

        byte[] testPayload = "cityhelp-rsa-self-test".getBytes(StandardCharsets.UTF_8);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(testPayload);
        byte[] signed = signature.sign();

        signature.initVerify(publicKey);
        signature.update(testPayload);
        if (!signature.verify(signed)) {
            throw new IllegalStateException("RSA key pair validation failed: public key cannot verify signature from private key");
        }
    }

    private void selfTestJwtSigning() {
        try {
            String jwt = io.jsonwebtoken.Jwts.builder()
                    .subject("health-check")
                    .signWith(privateKey, io.jsonwebtoken.Jwts.SIG.RS256)
                    .compact();

            io.jsonwebtoken.Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(jwt);
        } catch (Exception e) {
            throw new IllegalStateException("JWT self-test failed. Check RSA keys and algorithm support.", e);
        }
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
