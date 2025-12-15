package com.crudzaso.cityhelp.auth.infrastructure.controller;

import com.crudzaso.cityhelp.auth.application.LoginUserUseCase;
import com.crudzaso.cityhelp.auth.application.RefreshTokenUseCase;
import com.crudzaso.cityhelp.auth.application.exception.InvalidCredentialsException;
import com.crudzaso.cityhelp.auth.domain.enums.OAuthProvider;
import com.crudzaso.cityhelp.auth.domain.enums.UserRole;
import com.crudzaso.cityhelp.auth.domain.enums.UserStatus;
import com.crudzaso.cityhelp.auth.domain.model.RefreshToken;
import com.crudzaso.cityhelp.auth.domain.model.User;
import com.crudzaso.cityhelp.auth.infrastructure.security.RsaKeyProvider;
import com.crudzaso.cityhelp.auth.infrastructure.security.RsaKeyProviderTestKeys;
import com.crudzaso.cityhelp.auth.infrastructure.service.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String PRIVATE_KEY_ESCAPED = RsaKeyProviderTestKeys.PRIVATE_KEY.replace("\n", "\\n");
    private static final String PUBLIC_KEY_ESCAPED = RsaKeyProviderTestKeys.PUBLIC_KEY.replace("\n", "\\n");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RsaKeyProvider rsaKeyProvider;

    @MockBean
    private LoginUserUseCase loginUserUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private MetricsService metricsService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void jwtProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.rsa.private-key", () -> PRIVATE_KEY_ESCAPED);
        registry.add("app.jwt.rsa.public-key", () -> PUBLIC_KEY_ESCAPED);
        registry.add("app.jwt.rsa.key-id", () -> "integration-key");
    }

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUuid(java.util.UUID.randomUUID());
        sampleUser.setEmail("demo@example.com");
        sampleUser.setFirstName("Demo");
        sampleUser.setLastName("User");
        sampleUser.setRole(UserRole.ADMIN);
        sampleUser.setOAuthProvider(OAuthProvider.LOCAL);
        sampleUser.setStatus(UserStatus.ACTIVE);
        sampleUser.setIsVerified(true);
        sampleUser.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
    }

    @Test
    void loginEndpointReturnsSignedToken() throws Exception {
        RefreshToken refreshToken = new RefreshToken("refresh-token", sampleUser.getId(), LocalDateTime.now(ZoneOffset.UTC).plusDays(7));

        when(loginUserUseCase.execute(anyString(), anyString())).thenReturn(sampleUser);
        when(refreshTokenUseCase.generateNewToken(sampleUser.getId(), 7)).thenReturn(refreshToken);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"demo@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.refresh_token").value(refreshToken.getToken()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        String accessToken = jsonNode.get("access_token").asText();

        var claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith(rsaKeyProvider.getPublicKey())
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

        assertEquals(sampleUser.getEmail(), claims.getSubject());
        assertEquals(sampleUser.getId(), claims.get("user_id", Long.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void loginEndpointRejectsInvalidCredentials() throws Exception {
        when(loginUserUseCase.execute(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("invalid"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("incorrectos")));
    }
}
